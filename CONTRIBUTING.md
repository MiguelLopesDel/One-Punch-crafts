# Contributing to One Punch Crafts

One Punch Crafts is a proprietary, source-available project. It is not an
open-source project, and contributions are accepted only under the
[Contributor Copyright Assignment Agreement](CONTRIBUTOR_ASSIGNMENT.md).

## Before submitting

1. Read `LICENSE` and `CONTRIBUTOR_ASSIGNMENT.md` completely.
2. Confirm that you personally own the material or have written authority to
   assign it. Do not submit code copied from another project merely because
   that project is publicly visible.
3. Disclose every third-party component and its license in the pull request.
4. Do not include credentials, private information, generated build output,
   runtime data, or third-party binaries.
5. Use focused commits and run the relevant tests before opening the pull
   request.

## Required signature

Every commit in a contribution must contain:

```text
Signed-off-by: Full Legal Name <email@example.com>
```

`git commit -s` adds this line using your configured Git identity. Verify that
it contains your full legal name and an email address you control. The line is
an explicit acceptance mechanism for `CONTRIBUTOR_ASSIGNMENT.md`, not merely a
statement about code provenance.

The pull request author must also check the contributor-assignment acceptance
box in the pull request template. If a company or employer owns the work, an
authorized representative must contact the Owner before submission.

The repository's `Contributor agreement` check verifies the acceptance boxes
and sign-off lines. Maintainers must configure the default branch to require
that check before merging; bypassing it means the automated guard was not
used and may require a separate signed agreement.

The Owner may reject or close any contribution and is not required to use or
maintain accepted work.
