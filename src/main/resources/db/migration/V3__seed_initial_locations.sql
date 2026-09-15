-- Geographic scope explicitly supplied by the project owner. No commercial data is seeded.
INSERT INTO states (id, name, code)
VALUES ('db879b15-eb27-49db-bb00-b724a6032c8e', 'Rio de Janeiro', 'RJ');

INSERT INTO cities (id, state_id, name) VALUES
    ('623614c4-674b-4e6d-a252-2eafbd98be97', 'db879b15-eb27-49db-bb00-b724a6032c8e', 'Rio de Janeiro'),
    ('5c4cb935-52e1-4bf8-8d17-902dc0837c66', 'db879b15-eb27-49db-bb00-b724a6032c8e', 'Volta Redonda'),
    ('2f6cba12-af5c-4af6-96a6-da7184148f88', 'db879b15-eb27-49db-bb00-b724a6032c8e', 'Barra Mansa'),
    ('f9696e39-5c19-4211-8240-455d4ab1622c', 'db879b15-eb27-49db-bb00-b724a6032c8e', 'Resende');
