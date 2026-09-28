-- ---------------------------------------------------------------------------
-- V2 : reference categories + a small demo catalogue
-- Safe to delete if you want to start from an empty catalogue.
-- ---------------------------------------------------------------------------

INSERT INTO categories (name, description) VALUES
    ('Nutrition',      'Supplements, protein powders and wellness products'),
    ('Drinks',         'Beverages and energy drinks'),
    ('Personal Care',  'Skin, hair and body care products'),
    ('Food',           'Packaged food and groceries'),
    ('Household',      'Home care and cleaning products'),
    ('Other',          'Anything that does not fit the categories above');

INSERT INTO products (name, sku, description, mrp, volume_point, category_id, min_quantity, max_quantity, active)
SELECT 'Protein Powder', 'P001', 'Whey protein powder, 1 kg jar', 2000.00, 50,
       (SELECT id FROM categories WHERE name = 'Nutrition'), 0, 6, TRUE
UNION ALL
SELECT 'Multivitamin', 'P002', 'Multivitamin tablets, 60 count', 1000.00, 25,
       (SELECT id FROM categories WHERE name = 'Nutrition'), 0, 10, TRUE
UNION ALL
SELECT 'Omega 3', 'P003', 'Omega 3 fish oil capsules', 1500.00, 40,
       (SELECT id FROM categories WHERE name = 'Nutrition'), 0, 8, TRUE
UNION ALL
SELECT 'Energy Drink', 'P004', 'Energy drink, pack of 6', 2500.00, 75,
       (SELECT id FROM categories WHERE name = 'Drinks'), 0, 4, TRUE
UNION ALL
SELECT 'Shampoo', 'P005', 'Anti-dandruff shampoo, 650 ml', 800.00, 20,
       (SELECT id FROM categories WHERE name = 'Personal Care'), 0, 12, TRUE
UNION ALL
SELECT 'Green Tea', 'P006', 'Green tea, 100 tea bags', 1200.00, 30,
       (SELECT id FROM categories WHERE name = 'Food'), NULL, NULL, TRUE
UNION ALL
SELECT 'Hand Sanitizer', 'P007', 'Alcohol based hand sanitizer, 500 ml', 600.00, 15,
       (SELECT id FROM categories WHERE name = 'Household'), NULL, NULL, TRUE;
