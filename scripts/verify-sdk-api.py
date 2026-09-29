#!/usr/bin/env python3
"""Audit the packaged SDK without loading an Android ELF library on the host.

Run from any directory: python3 scripts/verify-sdk-api.py
Optionally pass --sdk-dir <extracted upstream SDK> to verify upstream provenance.
This is a source/export/packaging audit; run the Android instrumentation tests for
runtime marshalling, inference, capture, resource ownership and FeatureHub checks.
Only Python's standard library is required, including for the ELF symbol reader.
"""

import argparse
import hashlib
import json
import re
import struct
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "inspireface/src/main/java"
SDK = ROOT / "inspireface/sdk/1.2.4"
ABIS = {"armeabi-v7a": (1, 40), "arm64-v8a": (2, 183), "x86_64": (2, 62)}
# Public Android system libraries available at the library's minimum API level.
# A bundled C++ runtime or another private dependency would violate single-so packaging.
ANDROID_SYSTEM_LIBRARIES = {"libandroid.so", "libc.so", "libdl.so", "libjnigraphics.so",
                            "liblog.so", "libm.so", "libz.so"}


def uncomment(source):
    # Preserve string literals while removing both Java and C comments.
    return re.sub(r'"(?:\\.|[^"\\])*"|/\*.*?\*/|//[^\n]*',
                  lambda m: m[0] if m[0].startswith('"') else " ", source,
                  flags=re.S)


def normalized_type(value):
    return re.sub(r"\s*\[\s*\]\s*", "[]", value.strip())


def argument_types(arguments):
    result = []
    for argument in arguments.split(","):
        if not argument.strip():
            continue
        argument = re.sub(r"\bfinal\s+", "", argument.strip())
        match = re.fullmatch(r"(.+?)\s+(\w+)(\s*\[\s*\])?", argument)
        if not match:
            raise ValueError("Cannot parse Java argument: " + argument)
        result.append(normalized_type(match[1] + (match[3] or "")))
    return result


def public_signatures(source):
    """Read public declarations in the simple, top-level legacy facade classes.

    Parameter names and implementation-only native/synchronized modifiers are
    excluded; return types, static/final, field types and superclass are retained.
    """
    result = set()
    for match in re.finditer(r"\bpublic\s+([^;{}=]+)(?=[;{=])", uncomment(source)):
        declaration = re.sub(r"\b(?:native|synchronized)\s+", "", match[1].strip())
        declaration = re.sub(r"\s+", " ", declaration)
        if re.search(r"\bclass\b", declaration) and " implements " in declaration:
            declaration, interfaces = declaration.split(" implements ", 1)
            # Adding an interface is compatible; a previously implemented one
            # still appears independently in the baseline and cannot disappear.
            result.update("public implements " + interface.strip() for interface in interfaces.split(","))
        if "(" in declaration:
            prefix, tail = declaration.split("(", 1)
            args = tail.split(")", 1)[0]
            declaration = prefix.strip() + "(" + ",".join(argument_types(args)) + ")"
        else:
            declaration = re.sub(r"(\w+)\s*(\[\])$", r"[] \1", declaration)
            declaration = normalized_type(declaration)
            # Normalize both `float []data` and `float data[]`.
            declaration = re.sub(r"\[\](?=\w)", "[] ", declaration)
        result.add("public " + declaration)
    return sorted(result)


def elf_exports(path):
    """Return ELF class, machine, defined dynamic symbols and DT_NEEDED entries."""
    blob = path.read_bytes()
    if blob[:4] != b"\x7fELF" or blob[5] not in (1, 2):
        raise ValueError(str(path) + " is not a supported ELF")
    elf_class = blob[4]
    endian = "<" if blob[5] == 1 else ">"
    header = struct.unpack_from(endian + ("HHIIIIIHHHHHH" if elf_class == 1 else "HHIQQQIHHHHHH"), blob, 16)
    machine, section_offset, section_size, section_count = header[1], header[5], header[10], header[11]
    sections = [struct.unpack_from(endian + ("IIIIIIIIII" if elf_class == 1 else "IIQQQQIIQQ"),
                                   blob, section_offset + index * section_size)
                for index in range(section_count)]
    exports = set()
    dependencies = set()
    for section in sections:
        if section[1] not in (6, 11):  # SHT_DYNAMIC, SHT_DYNSYM
            continue
        strings = sections[section[6]]
        string_table = blob[strings[4]:strings[4] + strings[5]]
        for offset in range(section[4], section[4] + section[5], section[9]):
            if section[1] == 6:
                tag, value = struct.unpack_from(endian + ("iI" if elf_class == 1 else "qQ"), blob, offset)
                if tag == 1:  # DT_NEEDED
                    dependencies.add(string_table[value:string_table.index(b"\0", value)].decode("utf-8"))
                continue
            symbol = struct.unpack_from(endian + ("IIIBBH" if elf_class == 1 else "IBBHQQ"), blob, offset)
            name, info, defined = (symbol[0], symbol[3], symbol[5]) if elf_class == 1 else (symbol[0], symbol[1], symbol[3])
            if defined and info >> 4 in (1, 2):
                exports.add(string_table[name:string_table.index(b"\0", name)].decode("utf-8"))
    return elf_class, machine, exports, dependencies


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk-dir", type=Path, help="Unmodified extracted upstream SDK")
    args = parser.parse_args()
    errors = []

    def check(condition, message):
        if not condition:
            errors.append(message)

    manifest = json.loads((SDK / "api-manifest.json").read_text())
    native_file = JAVA / "com/insightface/sdk/inspireface/jni/Native.java"
    source = uncomment(native_file.read_text())
    declarations = {}
    for result, name, parameters in re.findall(r"public static native\s+(\w+)\s+(HF\w+)\(([^)]*)\);", source):
        declarations[name] = (result, argument_types(parameters))
    check(set(declarations) == set(manifest),
          "Manifest/Native methods differ: " + str(sorted(set(declarations) ^ set(manifest))))
    for name, specification in manifest.items():
        check(declarations.get(name) == (specification["return"], argument_types(",".join(specification["arguments"]))),
              name + " Java signature differs from upstream manifest")
    header = uncomment((SDK / "include/inspireface.h").read_text())
    c_api = set(re.findall(r"HYPER_CAPI_EXPORT\s+extern\s+\w+\s+(HF\w+)\s*\(", header))
    check(c_api == set(manifest), "C header/manifest methods differ: " + str(sorted(c_api ^ set(manifest))))

    checksums = dict((name, sha) for sha, name in
                     (line.split(None, 1) for line in (SDK / "SHA256SUMS").read_text().splitlines()))
    for target, upstream in [("api-manifest.json", "java/api-manifest.json"),
                             ("include/inspireface.h", "include/inspireface.h"),
                             ("include/intypedef.h", "include/intypedef.h"),
                             ("include/herror.h", "include/herror.h"),
                             ("version.txt", "version.txt")]:
        check(digest(SDK / target) == checksums[upstream], target + " differs from SDK checksum")
    for upstream, expected in checksums.items():
        if upstream.startswith("java/sources/") and upstream.endswith(".java"):
            path = JAVA / upstream[len("java/sources/"):]
            check(digest(path) == expected, str(path.relative_to(ROOT)) + " differs from generated SDK source")

    # All Java native methods (legacy capture and portable JNI) must resolve on every ABI.
    required_jni = set()
    for path in JAVA.rglob("*.java"):
        source = uncomment(path.read_text())
        package = re.search(r"\bpackage\s+([\w.]+);", source)[1]
        for name in re.findall(r"\bnative\s+\w+(?:\[\])?\s+(\w+)\s*\(", source):
            required_jni.add("Java_" + package.replace(".", "_") + "_" + path.stem + "_" + name.replace("_", "_1"))
    library_root = ROOT / "inspireface/libs"
    expected_libraries = {abi + "/libInspireFace.so" for abi in ABIS}
    packaged_files = {path.relative_to(library_root).as_posix() for path in library_root.rglob("*") if path.is_file()}
    check(packaged_files == expected_libraries,
          "Single-so inventory mismatch; missing: " + str(sorted(expected_libraries - packaged_files))
          + "; unexpected: " + str(sorted(packaged_files - expected_libraries)))
    upstream_libraries = {path[len("lib/"):] for path in checksums if path.startswith("lib/")}
    check(upstream_libraries == expected_libraries, "SDK checksum inventory is not the expected single-so package")
    library_audits = []
    for abi, architecture in ABIS.items():
        path = library_root / abi / "libInspireFace.so"
        check(digest(path) == checksums["lib/" + abi + "/libInspireFace.so"],
              str(path.relative_to(ROOT)) + " SDK checksum mismatch")
        elf_class, machine, exports, dependencies = elf_exports(path)
        check((elf_class, machine) == architecture, abi + "/libInspireFace.so ELF architecture mismatch")
        check(c_api <= exports, abi + " core library missing C exports: " + str(sorted(c_api - exports)))
        check(required_jni <= exports, abi + " core library missing JNI exports: " + str(sorted(required_jni - exports)))
        check(bool(dependencies), abi + " core library has no readable DT_NEEDED entries")
        check(dependencies <= ANDROID_SYSTEM_LIBRARIES,
              abi + " core library requires unprovided libraries: " + str(sorted(dependencies - ANDROID_SYSTEM_LIBRARIES)))
        library_audits.append(abi + ": libInspireFace.so; DT_NEEDED=" + ", ".join(sorted(dependencies)))

    # Release documentation is English and avoids a restricted vendor label.
    # Source identifiers and the unmodified upstream headers remain outside this prose check.
    documentation = set(ROOT.glob("README*"))
    documentation.update(path for path in (ROOT / "docs").rglob("*") if path.is_file())
    documentation.update(SDK.glob("README*"))
    documentation.update(SDK.glob("version.txt"))
    documentation.add(ROOT / "inspireface/src/main/assets/inspireface/version.txt")
    restricted_label = "m" + "nn"
    for path in sorted(documentation):
        if not path.is_file():
            continue
        prose = path.read_text()
        relative = path.relative_to(ROOT).as_posix()
        check(restricted_label not in prose.casefold(), relative + " contains the restricted vendor label")
        check(not re.search(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff\U00020000-\U000323af]", prose),
              relative + " contains Chinese text; release documentation must be English")

    baseline = json.loads((ROOT / "scripts/legacy-api-1.2.3.post5.json").read_text())
    for relative, signatures in baseline["classes"].items():
        path = JAVA / relative
        current = set(public_signatures(path.read_text())) if path.exists() else set()
        missing = set(signatures) - current
        check(not missing, relative + " removed/changed legacy API: " + str(sorted(missing)))

    if args.sdk_dir:
        check((args.sdk_dir / "SHA256SUMS").read_bytes() == (SDK / "SHA256SUMS").read_bytes(),
              "Upstream SHA256SUMS differs from archived SDK checksums")
        for relative, expected in checksums.items():
            check(digest(args.sdk_dir / relative) == expected, "Upstream checksum mismatch: " + relative)

    if errors:
        print("SDK API audit FAILED:", file=sys.stderr)
        for error in errors:
            print("  - " + error, file=sys.stderr)
        return 1
    for audit in library_audits:
        print(audit)
    print("PASS: %d C/Java API signatures, %d JNI symbols across %d single-so ABIs, 3 library hashes; %d legacy public declarations preserved; documentation constraints satisfied." %
          (len(manifest), len(required_jni), len(ABIS), sum(len(v) for v in baseline["classes"].values())))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, IndexError, struct.error) as error:
        print("SDK API audit FAILED: " + str(error), file=sys.stderr)
        sys.exit(1)
