# Beholder configuration

## Config structure

Config contains commands, which can have subcommands (and so on).
Commands can produce, consume and modify messages.
Messages are collections of arbitrary fields.

    flow {  # this command has no args, only subcommands
        switch $field {  # this command has both args and subcommands
            case ~cat~ {
                from udp 3820;  # this command has only arguments
                parse syslog;
            }
        }
        to stdout;
    }


## Config syntax

Command arguments can be expressed as literal words, quoted strings and regexps.

Quoted strings start with either `'` or `"`. There is no difference between the two.
Escaping is done with backslashes. Special characters: `\n`, `\r`, `\t`.
Backslashes prefixing any other characters are stripped off.

* `'this \' is a quote'` => this ' is a quote
* `'this \" is also a quote'` => this " is also a quote
* `'\z'` => z
* `'\n'` => newline character
* `'\\n'` => \n

Quoted strings may contain message field names, which are replaced with their values.
Some arguments to certain commands do not allow field names; be sure to validate your configs.

    'date: $date payload: $payload'

Simple syntax: `'$field'`. To prevent unwanted greediness: `'{$cat}astrophe'`.
Field names consist of alphanumeric characters (case-sensitive) and underscores.
Field names cannot start with numbers.

Regexps are recognized by a delimiter, which currently can only be `~`.
The delimiter currently cannot be escaped in the regexp.
Regexp are written in the form of `~body~modifiers`. Modifiers are optional.

    ~spaces are allowed~
    ~http://[a-z.]+~
    ~cat|dog~i

Literal word is a string of non-whitespace characters that is not a quoted string or regexp.
Literal words may contain field names, which are replaced with their values.
There is no escaping in literal words.

Note also that literal `{$field}` is invalid (it will be parsed as subcommand block),
while `$field` and quoted `'{$field}'` are valid.

Example: `127.0.0.1:1234`.
