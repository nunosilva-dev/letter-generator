"""Reject release tags that do not match the POM or committed main history."""

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def check_release(tag: str, repository: Path) -> str:
    """Return the eligible version, or raise ValueError with a useful failure reason."""
    if not re.fullmatch(r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)", tag):
        raise ValueError("Release tags must use stable vX.Y.Z format (for example v1.0.0).")

    version = ET.parse(repository / "pom.xml").findtext(
        "{http://maven.apache.org/POM/4.0.0}version"
    )
    if tag[1:] != version:
        raise ValueError(f"Tag {tag} does not match POM version {version}.")

    def git(*arguments):
        return subprocess.run(
            ["git", *arguments], cwd=repository, capture_output=True, text=True
        )

    release = git("rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}")
    if release.returncode:
        raise ValueError(f"Cannot resolve release tag {tag}; fetch tags and full history.")
    if release.stdout.strip() != git("rev-parse", "HEAD").stdout.strip():
        raise ValueError(f"Release tag {tag} does not point to HEAD.")

    main = git("rev-parse", "--verify", "refs/remotes/origin/main^{commit}")
    if main.returncode:
        raise ValueError("Cannot resolve origin/main; fetch the remote and full history.")
    if git("merge-base", "--is-ancestor", "HEAD", main.stdout.strip()).returncode:
        raise ValueError(f"Release commit for {tag} is not on origin/main.")
    return version


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: python3 .github/scripts/check_release.py vX.Y.Z")
    try:
        result = check_release(sys.argv[1], Path.cwd())
    except (ValueError, OSError, ET.ParseError) as error:
        sys.exit(f"Release rejected: {error}")
    print(f"Release {result} matches the POM and is committed on origin/main.")
