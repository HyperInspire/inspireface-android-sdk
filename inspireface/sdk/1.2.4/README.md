# Imported SDK contract

Source: the user-supplied InspireFace 1.2.4 single-library Android SDK archive.

Archive SHA-256: `3ab827f17891fb0065b04c7778d230497e44d2d3008a0add0d79b913275cf422`

Native version: 1.2.4; Android wrapper/publication version: 1.2.4.post1.

`SHA256SUMS`, the three public C headers, `api-manifest.json` and `version.txt` are
unmodified copies from the archive. The checksum list also describes upstream
files that are not needed in this Android project.

All six `java/sources/.../jni/*.java` files are imported unchanged into the
library source tree. Each ABI ships only `libInspireFace.so`, containing all
public C functions, Android JNI functions and portable JNI functions. The
upstream loader selects `InspireFace` on Android. The upstream JAR is intentionally
not added alongside these sources, since that would duplicate the same Java classes.

The eight Android capture/snapshot source files are integrated into the public
Android API. Local additions validate capture configuration ranges and expose
owned snapshot result copies. Existing model packs are retained; the new archive
does not include model assets.

Run `python3 scripts/verify-sdk-api.py` from the repository root to verify the
complete C/Java manifest, generated source hashes, legacy API compatibility,
library hashes/architectures and every declared JNI symbol. An optional
`--sdk-dir` argument additionally verifies every original archive entry.
