# `keep` — removes unnecessary message fields

    keep $field [$field2 ...]

Only keeps certain fields in the message. All fields that are not specified in arguments are removed.

Example config:

    from timer;
    dump 'BEFORE> ';

    keep $date $payload;
    dump 'AFTER> '

Example output:

    BEFORE> #7
    BEFORE> $date=2020-02-20T17:25:55+03:00
    BEFORE> $from=beholder://timer
    BEFORE> $payload=A large hedgehog entered the building
    BEFORE> $program=beholder
    AFTER> #7
    AFTER> $date=2020-02-20T17:25:55+03:00
    AFTER> $payload=A large hedgehog entered the building
