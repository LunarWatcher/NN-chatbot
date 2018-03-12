# Contributing

Contributions are always welcome, whether it's as a pull request, fixing typos, documentation writing, an issue or in any other form. There are, however, some contribution *guidelines*. They aren't rules ("anything that doesn't follow these standards will be denied") but they are here to help creating good contributions.

In order to contribute, you'll need a GitHub account. If you plan on contributing to the code, installing a Python IDE and Git is a good idea too. This allows you to make changes in multiple files in a single commit, which also saves some clicking compared to using the browser version of GitHub.

## Extensive pull requests

Extensive pull requests are prefered. If you're going to make changes, put all of them in one pull request. This saves time in having to pull, because 1 pull takes less time than 5. This is because the code is reviewed before it's merged (to avoid conflicts, fix missed bugs, etc.).

Obviously, if a single-change commit in a PR is of high priority, it'll be merged into master as soon as possible. Note that, again, these are only guidelines and not rules. There are other cases where single-change commits will be merged into master, but multiple of these in a row from the same author are better off in a single PR.

## Issues

There are many different types of issues. Note that only Python 3.5 and above is supported. Issues related to lower versions will be closed as those versions of Python aren't being developed for. 

Please search for similar issues before you post a new one. Duplicate issues will be closed.

Bug reports requires an [MCVE](https://stackoverflow.com/help/mcve). Why? Because bugs can't be debugged without code and/or stacktraces. Since this is a standalone project (and not a library), code isn't necessary (since the stacktrace points to the lines in the code that cause problems.

Feature requests are accepted, as long as they haven't been requested before. Adding new features to the bot is one of the top priorities.

## Documentation writing

Documentation is necessary to have (in most cases). Feel free to expand the documentation available. Anything beyond quick setup goes in the wiki, though setup belongs there too. The wiki hasn't been created (yet), but it is available. 

## Issue tagging

There are multiple tags that are used, which are made to help potential contributors find issues they can help with. Even though this repo has some ML components, not everything is. You'll see the `nn/ml` tag on issues relating to it. In addition, there's a `regex` and `command` label to help separate issues into different categories. There are of course priority tags (low, medium and high), `bug`, `feature request`, `regex`, and a few meta tags. In addition, there's `good first issue`. If you're looking to contribute (and have no idea where to start) looking for issues in this tag is a good place to start.

<sub>"tags" are called "labels" on GitHub</sub>
