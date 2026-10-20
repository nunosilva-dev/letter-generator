"""Exercise release eligibility against real Git repositories, without credentials."""

import subprocess
import tempfile
import unittest
from pathlib import Path

from check_release import check_release


class CheckReleaseTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.repository = Path(self.directory.name)
        self.git("init", "--initial-branch=main")
        self.write_version("1.0.0")
        self.commit()
        self.git("update-ref", "refs/remotes/origin/main", "HEAD")
        self.git("tag", "v1.0.0")

    def git(self, *arguments):
        return subprocess.run(
            [
                "git",
                "-c", "user.name=Release Test",
                "-c", "user.email=release-test@example.invalid",
                "-c", "commit.gpgsign=false",
                "-c", "tag.gpgSign=false",
                *arguments,
            ],
            cwd=self.repository, check=True, capture_output=True, text=True,
        ).stdout.strip()

    def write_version(self, version):
        (self.repository / "pom.xml").write_text(
            '<project xmlns="http://maven.apache.org/POM/4.0.0">'
            f"<version>{version}</version></project>", encoding="utf-8",
        )

    def commit(self):
        self.git("add", "pom.xml")
        self.git("commit", "--message=Release fixture")

    def test_accepts_matching_tag_on_main(self):
        self.assertEqual("1.0.0", check_release("v1.0.0", self.repository))

    def test_accepts_a_release_commit_already_merged_into_main(self):
        release_commit = self.git("rev-parse", "HEAD")
        self.write_version("1.0.1")
        self.commit()
        self.git("update-ref", "refs/remotes/origin/main", "HEAD")
        self.git("checkout", "--detach", release_commit)
        self.assertEqual("1.0.0", check_release("v1.0.0", self.repository))

    def test_accepts_annotated_tags(self):
        self.git("tag", "--delete", "v1.0.0")
        self.git("tag", "--annotate", "v1.0.0", "--message=Release 1.0.0")
        self.assertEqual("1.0.0", check_release("v1.0.0", self.repository))

    def test_rejects_tags_that_are_not_stable_versions(self):
        for tag in ["1.0.0", "v1.0", "v1.0.0-SNAPSHOT", "v1.0.0-rc1",
                    "v01.0.0", "v1.0.0\n", "v1.0.0;echo unsafe"]:
            with self.subTest(tag=tag), self.assertRaisesRegex(ValueError, "vX.Y.Z"):
                check_release(tag, self.repository)

    def test_rejects_tag_and_pom_version_mismatch(self):
        with self.assertRaisesRegex(ValueError, "does not match POM version"):
            check_release("v1.0.1", self.repository)

    def test_rejects_snapshot_pom(self):
        self.write_version("1.0.0-SNAPSHOT")
        with self.assertRaisesRegex(ValueError, "does not match POM version"):
            check_release("v1.0.0", self.repository)

    def test_rejects_a_commit_outside_main(self):
        self.git("checkout", "-b", "unmerged")
        self.write_version("1.0.1")
        self.commit()
        self.git("tag", "v1.0.1")
        with self.assertRaisesRegex(ValueError, "not on origin/main"):
            check_release("v1.0.1", self.repository)

    def test_rejects_a_tag_pointing_to_a_different_commit(self):
        self.write_version("1.0.1")
        self.commit()
        self.write_version("1.0.0")
        with self.assertRaisesRegex(ValueError, "does not point to HEAD"):
            check_release("v1.0.0", self.repository)

    def test_rejects_a_missing_tag(self):
        self.git("tag", "--delete", "v1.0.0")
        with self.assertRaisesRegex(ValueError, "Cannot resolve release tag"):
            check_release("v1.0.0", self.repository)

    def test_rejects_missing_main_history(self):
        self.git("update-ref", "-d", "refs/remotes/origin/main")
        with self.assertRaisesRegex(ValueError, "Cannot resolve origin/main"):
            check_release("v1.0.0", self.repository)


if __name__ == "__main__":
    unittest.main()
