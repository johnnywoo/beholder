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

Example output:

    ORIGINAL> #7
    ORIGINAL> $date=2020-02-20T17:21:20+03:00
    ORIGINAL> $from=beholder://timer
    ORIGINAL> $payload=A tiny mouse got paid by the zoo
    ORIGINAL> $program=beholder
    JSON> #7
    JSON> $date=2020-02-20T17:21:20+03:00
    JSON> $from=beholder://timer
    JSON> $payload={"date":"2020-02-20T17:21:20+03:00","from":"beholder://timer","payload":"A tiny mouse got paid by the zoo","program":"beholder"}
    JSON> $program=beholder
