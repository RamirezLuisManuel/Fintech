-- V11: Corrección de códigos postales oficiales SEPOMEX para comunidades de Dolores Hidalgo, Gto.

UPDATE cat_colonias c
SET codigo_postal = v.cp
FROM (VALUES
    ('Xoconoxtle el Grande', '37830'),
    ('Xoconoxtle el Chico', '37830'),
    ('Remátalo', '37830'),
    ('Don Pedro', '37830'),
    ('El Saucillo', '37848'),
    ('Río Laja', '37810'),
    ('El Llanito', '37840'),
    ('San Gabriel', '37817'),
    ('La Erre', '37842'),
    ('Adjuntas del Río', '37814'),
    ('San Marcos de Abajo', '37848'),
    ('San Marcos de Arriba', '37848'),
    ('San Antón de las Minas', '37849'),
    ('San Martín de Terreros', '37838'),
    ('Peñuelitas', '37814'),
    ('Santa Bárbara', '37832'),
    ('Tequisquiapan', '37814'),
    ('Trancas', '37828'),
    ('La Ventilla', '37842'),
    ('El Gallinero', '37829'),
    ('Tierra Blanca', '37843'),
    ('Zamarripa', '37825')
) AS v(nombre, cp)
JOIN cat_municipios m ON m.nombre = 'Dolores Hidalgo'
WHERE c.municipio_id = m.id AND c.nombre = v.nombre;
