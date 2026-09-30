#!/usr/bin/env bash
# Prints Minecraft / Fabric API signatures (javap) for the classes listed in ci/api-classes.txt.
# Used to check the exact Mojang-mapped names when porting the mod to another Minecraft version.
# Each line: <fully.qualified.ClassName> [optional grep -E filter]
set -uo pipefail
cd "$(dirname "$0")/.."

CLASSPATH=$(./gradlew -q printClasspath | sed -n 's/^CLASSPATH=//p')

if [[ -z "$CLASSPATH" ]]; then
	echo "Could not resolve the classpath" >&2
	exit 1
fi

while read -r class filter; do
	[[ -z "$class" || "$class" == \#* ]] && continue
	echo "===== $class ${filter:+[$filter]}"

	if ! output=$(javap -p -cp "$CLASSPATH" "$class" 2>&1); then
		echo "!! not found"
		continue
	fi

	if [[ -n "$filter" ]]; then
		grep -E -- "$filter" <<< "$output" || echo "!! no member matches the filter"
	else
		echo "$output"
	fi
done < ci/api-classes.txt
