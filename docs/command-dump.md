# `dump` — troubleshooting output

    dump [<prefix>];

This command writes full descriptions of incoming messages to stdout.
This way you can inspect messages at particular places of your config
if something in there doesn't do what you're expecting it to.

    flow {
        from timer;
        dump 'ORIGINAL> ';
        set $payload json;
        dump 'JSON> ';
    }
