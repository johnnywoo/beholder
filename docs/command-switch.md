# `switch` — conditional processing

    switch 'template with $fields' {
        case 'some-value' {
            <subcommands>
        }
        case ~regexp~ {
            <subcommands>
        }
        default {
            <subcommands>
        }
    }

Subcommands of `switch`: `case`, `default`.

Subcommands of `case`/`default`: all commands are allowed.

`case` regexps are matched against the template provided as the argument to `switch`.
First matching `case` wins: its subcommands receive the message.
If there was no match, an optional `default` block receives the message.
There can be multiple `case` blocks, but only one `default`, and it must be the last block in `switch`.

The template can be a literal, quoted string or a regexp.

If there is no `default` subcommand, `switch` behaves as if it has an empty `default`.
Unmatched messages are just emitted out of the `switch` with no modifications.

`switch` can work as an if-statement:

    switch $host { case ~.~ {} default {drop} }
    to stdout; # Only prints messages with non-empty $host

Although `from` subcommand is permitted inside `case`/`default`, its use there is discouraged.
Messages emitted inside `case`/`default` ignore conditions and are emitted out of `switch`.

If your regexp has named groups, those groups will be placed as fields into the message:

    switch $program {
        case ~^nginx-(?<kind>access|error)$~ {
            # $kind now is either 'access' or 'error'
        }
    }
