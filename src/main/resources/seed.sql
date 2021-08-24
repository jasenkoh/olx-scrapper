create database `olx-scrapper`;
create user 'olxscrapper'@'%'  identified by '<PASSWORD>';
grant all on `olx-scrapper`.* to 'olxscrapper'@'%';

INSERT INTO
    filter(query_params, search_page_name, is_active)
VALUES
('&kanton=3&grad%5B%5D=4944&kategorija=24&id=2&stanje=0&okucnica-kvadratura_min=700','Kuca Tuzla', true),
('&kanton=7&grad%5B%5D=3017&kategorija=29&id=2&stanje=0', 'Zemljiste Mostar', true),
('&kanton=3&grad%5B%5D=4944&kategorija=29&id=2&stanje=0', 'Zemljiste Tuzla', true);