#!/usr/bin/env python3
"""Create a release commit, tag, and push to GitHub.

Usage:
    python3 release.py <version> <description.md>

Example:
    python3 release.py v1.1.1-pre docs/release-notes/v1.1.1-pre.md
"""

import argparse
import subprocess
import sys
from pathlib import Path


def run(cmd: list[str], check: bool = True) -> subprocess.CompletedProcess:
    """Run a shell command and return the result."""
    return subprocess.run(cmd, check=check, text=True, capture_output=True)


def get_git_root() -> Path:
    """Return the repository root directory."""
    result = run(["git", "rev-parse", "--show-toplevel"])
    return Path(result.stdout.strip())


def ensure_clean_working_tree() -> bool:
    """Check whether the working tree has any changes."""
    result = run(["git", "status", "--porcelain"])
    return result.stdout.strip() == ""


def tag_exists(tag: str) -> bool:
    """Check whether a Git tag already exists."""
    result = run(["git", "rev-parse", "--verify", f"refs/tags/{tag}"], check=False)
    return result.returncode == 0


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Commit, tag, and push a release to GitHub."
    )
    parser.add_argument("version", help="Version number to release (used as the tag name)")
    parser.add_argument("description", help="Path to a Markdown file used as the tag message")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the commands that would run without executing them",
    )
    parser.add_argument(
        "--remote",
        default="origin",
        help="Git remote to push to (default: origin)",
    )
    parser.add_argument(
        "--branch",
        default="",
        help="Branch to push (default: current branch)",
    )

    args = parser.parse_args()

    version = args.version
    desc_path = Path(args.description)

    if not desc_path.exists():
        print(f"Error: description file not found: {desc_path}", file=sys.stderr)
        return 1

    if not desc_path.is_file():
        print(f"Error: description path is not a file: {desc_path}", file=sys.stderr)
        return 1

    tag_name = version
    commit_message = f"Release {version}"
    tag_message = desc_path.read_text(encoding="utf-8")

    try:
        get_git_root()
    except subprocess.CalledProcessError:
        print("Error: not inside a Git repository", file=sys.stderr)
        return 1

    if tag_exists(tag_name):
        print(f"Error: tag '{tag_name}' already exists", file=sys.stderr)
        return 1

    if args.branch:
        branch = args.branch
    else:
        branch = run(["git", "branch", "--show-current"]).stdout.strip()
        if not branch:
            print("Error: could not determine current branch", file=sys.stderr)
            return 1

    commands: list[list[str]] = []

    if ensure_clean_working_tree():
        print("Working tree is clean; skipping commit.")
    else:
        commands.extend([
            ["git", "add", "-A"],
            ["git", "commit", "-m", commit_message],
        ])

    commands.extend([
        ["git", "tag", "-a", tag_name, "-m", tag_message],
        ["git", "push", args.remote, branch],
        ["git", "push", args.remote, tag_name],
    ])

    if args.dry_run:
        print("Dry run - would execute:")
        for cmd in commands:
            print("  " + " ".join(cmd))
        return 0

    for cmd in commands:
        print("$ " + " ".join(cmd))
        result = run(cmd, check=False)
        if result.stdout:
            print(result.stdout, end="")
        if result.stderr:
            print(result.stderr, end="", file=sys.stderr)
        if result.returncode != 0:
            print(f"Error: command failed with exit code {result.returncode}", file=sys.stderr)
            return result.returncode

    print(f"\nReleased {version} to {args.remote}/{branch}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
