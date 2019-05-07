# Beholder

Beholder is a log processor. Its purpose is to receive, process, transfer and export log messages.

WARNING: Beholder has no stable version yet.
Config syntax, commands, options, behaviour, everything is going to be changed without any backwards compatibility.

A somewhat reliable version is 0.1.222. You should disable the misguided manual GC feature when using that version
(put `extra_gc_interval_seconds 0;` in your config).

More recent versions contain lots of untested code and bugs!


## Usage

For a quick start, use [a premade Docker image](https://hub.docker.com/r/johnnywoo/beholder/builds):

    docker run --rm -ti johnnywoo/beholder:0.1.385 beholder --config 'from timer; to stdout'

With that example config you should see Beholder print randomized messages every second.

    usage: beholder
     -c,--config <text>        Use config from the argument
        --dump-instructions    Dump conveyor instructions for debugging
     -f,--config-file <file>   Use config from a file
     -h,--help                 Show usage
     -l,--log <file>           Internal log file
     -q,--quiet                Do not print internal log into stdout
     -t,--test                 Config test: syntax and minimal validation
     -v,--version              Show version


## Recipes and examples

Here are some example configs. You should look through these to familiarize yourself with Beholder.

Listen on a UDP port and write every incoming packet into a file, separated by newlines:

    flow {
        from udp 1234;
        to file '.../from-udp-1234.log';
    }

Receive nginx access log over UDP and write it into a file:

    flow {
        from udp 3820;
        parse syslog;
        to file '.../access.log';
    }

Send internal beholder log as syslog over UDP:

    flow {
        from internal-log;
        set $payload syslog;
        to udp 1234;
    }

Send internal stats (metrics) into Influx over UDP:

    flow {
        from timer 30 seconds;

        # These fields will become tags in Influx
        set $host host;
        set $tag value;
        keep $host $tag; # Do not create useless tags like 'date'

        parse beholder-stats;
        set $payload $influxLineProtocolPayload;
        to udp influxdb-host:8089;
    }


### Config commands

Message sources and destinations:

* [`from` — produces messages from various sources](docs/command-from.md)
* [`to` — sends messages to destinations](docs/command-to.md)
* [`dump` — troubleshooting output](docs/command-dump.md)

Message manipulation:

* [`set` — puts values into message fields](docs/command-set.md)
* [`keep` — removes unnecessary message fields](docs/command-keep.md)
* [`drop` — destroys messages](docs/command-drop.md)
* [`parse` — populates message fields according to some format](docs/command-parse.md)

Control structures:

* [`flow` — creates isolated flows of messages](docs/command-flow.md)
* [`tee` — applies commands to copies of messages](docs/command-tee.md)
* [`join` — produces messages from subcommands](docs/command-join.md)
* [`switch` — conditional processing](docs/command-switch.md)


### More docs

* [Settings — global configuration options](docs/settings.md)
* [Config structure and syntax](docs/config-syntax.md)
* [Building Beholder](docs/building-beholder.md)
