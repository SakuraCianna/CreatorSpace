alter table inspiration_cards
    drop constraint ck_inspiration_cards_type;

alter table inspiration_cards
    add constraint ck_inspiration_cards_type
        check (card_type in ('IMAGE', 'TEXT', 'PROMPT', 'CODE', 'LINK', 'SKETCH', 'REFERENCE'));
