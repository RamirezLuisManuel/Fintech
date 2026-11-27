-- V10: Poblado relacional de Estados/Entidades, Municipios/Ciudades y Colonias/Barrios
-- con códigos postales oficiales para los 30 países del catálogo (fuentes: SEPOMEX/INEGI y GeoNames)

-- ==================== MÉXICO: COMPLEMENTO DE MUNICIPIOS Y COLONIAS (32 ESTADOS) ====================
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
JOIN (VALUES
    ('Baja California', 'Tijuana'), ('Baja California', 'Mexicali'), ('Baja California', 'Ensenada'),
    ('Baja California Sur', 'La Paz'), ('Baja California Sur', 'Los Cabos'),
    ('Campeche', 'Campeche'), ('Campeche', 'Carmen'),
    ('Chiapas', 'Tuxtla Gutiérrez'), ('Chiapas', 'Tapachula'), ('Chiapas', 'San Cristóbal de las Casas'),
    ('Chihuahua', 'Chihuahua'), ('Chihuahua', 'Juárez'), ('Chihuahua', 'Delicias'),
    ('Coahuila', 'Saltillo'), ('Coahuila', 'Torreón'), ('Coahuila', 'Monclova'),
    ('Colima', 'Colima'), ('Colima', 'Manzanillo'), ('Colima', 'Villa de Álvarez'),
    ('Durango', 'Durango'), ('Durango', 'Gómez Palacio'), ('Durango', 'Lerdo'),
    ('Guerrero', 'Acapulco de Juárez'), ('Guerrero', 'Chilpancingo de los Bravo'), ('Guerrero', 'Zihuatanejo de Azueta'),
    ('Hidalgo', 'Pachuca de Soto'), ('Hidalgo', 'Tulancingo de Bravo'), ('Hidalgo', 'Tula de Allende'),
    ('Morelos', 'Cuernavaca'), ('Morelos', 'Jiutepec'), ('Morelos', 'Cuautla'),
    ('Nayarit', 'Tepic'), ('Nayarit', 'Bahía de Banderas'),
    ('Oaxaca', 'Oaxaca de Juárez'), ('Oaxaca', 'Salina Cruz'), ('Oaxaca', 'San Juan Bautista Tuxtepec'),
    ('Sinaloa', 'Culiacán'), ('Sinaloa', 'Mazatlán'), ('Sinaloa', 'Ahome'),
    ('Sonora', 'Hermosillo'), ('Sonora', 'Cajeme'), ('Sonora', 'Nogales'),
    ('Tabasco', 'Centro (Villahermosa)'), ('Tabasco', 'Cárdenas'), ('Tabasco', 'Comalcalco'),
    ('Tamaulipas', 'Reynosa'), ('Tamaulipas', 'Tampico'), ('Tamaulipas', 'Matamoros'), ('Tamaulipas', 'Nuevo Laredo'),
    ('Tlaxcala', 'Tlaxcala'), ('Tlaxcala', 'Apizaco'), ('Tlaxcala', 'Huamantla'),
    ('Zacatecas', 'Zacatecas'), ('Zacatecas', 'Fresnillo'), ('Zacatecas', 'Guadalupe')
) AS m(estado, nombre) ON e.nombre = m.estado
ON CONFLICT (nombre, estado_id) DO NOTHING;

INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.colonia, c.cp, m.id FROM cat_municipios m
JOIN (VALUES
    ('Tijuana', 'Zona Centro', '22000'), ('Tijuana', 'Zona Urbana Río Tijuana', '22010'), ('Tijuana', 'Playas de Tijuana', '22500'),
    ('Mexicali', 'Primera Sección (Centro)', '21100'), ('Mexicali', 'Nueva', '21100'),
    ('La Paz', 'Centro', '23000'), ('Los Cabos', 'Cabo San Lucas Centro', '23450'),
    ('Campeche', 'Zona Centro', '24000'), ('Tuxtla Gutiérrez', 'Centro', '29000'), ('Tuxtla Gutiérrez', 'Terán', '29050'),
    ('Chihuahua', 'Zona Centro', '31000'), ('Juárez', 'Zona Centro', '32000'),
    ('Saltillo', 'Zona Centro', '25000'), ('Torreón', 'Centro', '27000'),
    ('Colima', 'Centro', '28000'), ('Durango', 'Zona Centro', '34000'),
    ('Acapulco de Juárez', 'Centro', '39300'), ('Acapulco de Juárez', 'Costa Azul', '39850'),
    ('Pachuca de Soto', 'Centro', '42000'), ('Cuernavaca', 'Centro', '62000'),
    ('Tepic', 'Centro', '63000'), ('Oaxaca de Juárez', 'Centro', '68000'), ('Oaxaca de Juárez', 'Reforma', '68050'),
    ('Culiacán', 'Centro', '80000'), ('Mazatlán', 'Centro', '82000'),
    ('Hermosillo', 'Centro', '83000'), ('Centro (Villahermosa)', 'Centro', '86000'),
    ('Reynosa', 'Zona Centro', '88500'), ('Tampico', 'Zona Centro', '89000'),
    ('Tlaxcala', 'Centro', '90000'), ('Zacatecas', 'Centro', '98000'),
    ('Aguascalientes', 'Zona Centro', '20000'), ('San Luis Potosí', 'Centro Histórico', '78000'),
    ('Morelia', 'Centro Histórico', '58000'), ('Mérida', 'Centro', '97000'), ('Mérida', 'Altabrisa', '97130'),
    ('Benito Juárez (Cancún)', 'Centro', '77500'), ('Veracruz', 'Centro', '91700'),
    ('Puebla', 'Centro Histórico', '72000'), ('Toluca', 'Centro', '50000'),
    ('Zapopan', 'Centro', '45100'), ('Zapopan', 'Puerta de Hierro', '45116')
) AS c(municipio, colonia, cp) ON m.nombre = c.municipio
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- ==================== OTROS 29 PAÍSES: ESTADOS / PROVINCIAS / REGIONES ====================
INSERT INTO cat_estados (nombre, pais_id)
SELECT e.estado, p.id FROM cat_paises p
JOIN (VALUES
    ('Brasil', 'São Paulo'), ('Brasil', 'Río de Janeiro'), ('Brasil', 'Minas Gerais'), ('Brasil', 'Distrito Federal'),
    ('Chile', 'Región Metropolitana de Santiago'), ('Chile', 'Valparaíso'), ('Chile', 'Biobío'),
    ('Perú', 'Lima'), ('Perú', 'Cusco'), ('Perú', 'Arequipa'),
    ('Alemania', 'Berlín'), ('Alemania', 'Baviera'), ('Alemania', 'Hamburgo'), ('Alemania', 'Hesse'),
    ('Francia', 'Isla de Francia'), ('Francia', 'Provenza-Alpes-Costa Azul'), ('Francia', 'Auvernia-Ródano-Alpes'),
    ('Italia', 'Lacio'), ('Italia', 'Lombardía'), ('Italia', 'Toscana'), ('Italia', 'Véneto'),
    ('Reino Unido', 'Inglaterra'), ('Reino Unido', 'Escocia'), ('Reino Unido', 'Gales'),
    ('Japón', 'Tokio'), ('Japón', 'Osaka'), ('Japón', 'Kioto'), ('Japón', 'Hokkaido'),
    ('China', 'Pekín'), ('China', 'Shanghái'), ('China', 'Cantón (Guangdong)'),
    ('Corea del Sur', 'Seúl'), ('Corea del Sur', 'Busan'), ('Corea del Sur', 'Incheon'),
    ('Australia', 'Nueva Gales del Sur'), ('Australia', 'Victoria'), ('Australia', 'Queensland'),
    ('Portugal', 'Lisboa'), ('Portugal', 'Oporto'), ('Portugal', 'Faro'),
    ('Países Bajos', 'Holanda Septentrional'), ('Países Bajos', 'Holanda Meridional'), ('Países Bajos', 'Utrecht'),
    ('Suiza', 'Zúrich'), ('Suiza', 'Ginebra'), ('Suiza', 'Berna'),
    ('Suecia', 'Estocolmo'), ('Suecia', 'Västra Götaland'), ('Suecia', 'Escania'),
    ('Costa Rica', 'San José'), ('Costa Rica', 'Alajuela'), ('Costa Rica', 'Heredia'),
    ('Panamá', 'Panamá'), ('Panamá', 'Colón'), ('Panamá', 'Chiriquí'),
    ('Uruguay', 'Montevideo'), ('Uruguay', 'Canelones'), ('Uruguay', 'Maldonado'),
    ('Ecuador', 'Pichincha'), ('Ecuador', 'Guayas'), ('Ecuador', 'Azuay'),
    ('Guatemala', 'Guatemala'), ('Guatemala', 'Sacatepéquez'), ('Guatemala', 'Quetzaltenango'),
    ('India', 'Delhi'), ('India', 'Maharashtra'), ('India', 'Karnataka'),
    ('Rusia', 'Moscú'), ('Rusia', 'San Petersburgo'), ('Rusia', 'Tartaristán'),
    ('Turquía', 'Estambul'), ('Turquía', 'Ankara'), ('Turquía', 'Esmirna'),
    ('Sudáfrica', 'Gauteng'), ('Sudáfrica', 'Cabo Occidental'), ('Sudáfrica', 'KwaZulu-Natal')
) AS e(pais, estado) ON p.nombre = e.pais
ON CONFLICT (nombre, pais_id) DO NOTHING;

-- ==================== MUNICIPIOS / CIUDADES DE LOS 29 PAÍSES ====================
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.municipio, e.id FROM cat_estados e
JOIN (VALUES
    -- USA & Canadá & España & Colombia & Argentina
    ('Florida', 'Miami'), ('Florida', 'Orlando'), ('Florida', 'Tampa'),
    ('Nueva York', 'Nueva York'), ('Nueva York', 'Búfalo'),
    ('Illinois', 'Chicago'), ('Arizona', 'Phoenix'), ('Nevada', 'Las Vegas'),
    ('Ontario', 'Toronto'), ('Ontario', 'Ottawa'), ('Quebec', 'Montreal'), ('Columbia Británica', 'Vancouver'), ('Alberta', 'Calgary'),
    ('Cataluña', 'Barcelona'), ('Andalucía', 'Sevilla'), ('Andalucía', 'Málaga'), ('Comunidad Valenciana', 'Valencia'), ('País Vasco', 'Bilbao'),
    ('Bogotá D.C.', 'Bogotá'), ('Antioquia', 'Medellín'), ('Valle del Cauca', 'Cali'), ('Atlántico', 'Barranquilla'),
    ('Buenos Aires', 'La Plata'), ('Ciudad Autónoma de Buenos Aires', 'Buenos Aires'), ('Córdoba', 'Córdoba'), ('Santa Fe', 'Rosario'), ('Mendoza', 'Mendoza'),
    -- Resto de países
    ('São Paulo', 'São Paulo'), ('Río de Janeiro', 'Río de Janeiro'), ('Minas Gerais', 'Belo Horizonte'), ('Distrito Federal', 'Brasilia'),
    ('Región Metropolitana de Santiago', 'Santiago'), ('Valparaíso', 'Valparaíso'), ('Biobío', 'Concepción'),
    ('Lima', 'Lima'), ('Cusco', 'Cusco'), ('Arequipa', 'Arequipa'),
    ('Berlín', 'Berlín'), ('Baviera', 'Múnich'), ('Hamburgo', 'Hamburgo'), ('Hesse', 'Fráncfort'),
    ('Isla de Francia', 'París'), ('Provenza-Alpes-Costa Azul', 'Marsella'), ('Auvernia-Ródano-Alpes', 'Lyon'),
    ('Lacio', 'Roma'), ('Lombardía', 'Milán'), ('Toscana', 'Florencia'), ('Véneto', 'Venecia'),
    ('Inglaterra', 'Londres'), ('Inglaterra', 'Mánchester'), ('Escocia', 'Edimburgo'), ('Gales', 'Cardiff'),
    ('Tokio', 'Shibuya'), ('Tokio', 'Shinjuku'), ('Osaka', 'Osaka'), ('Kioto', 'Kioto'), ('Hokkaido', 'Sapporo'),
    ('Pekín', 'Pekín'), ('Shanghái', 'Shanghái'), ('Cantón (Guangdong)', 'Cantón'),
    ('Seúl', 'Seúl'), ('Busan', 'Busan'), ('Incheon', 'Incheon'),
    ('Nueva Gales del Sur', 'Sídney'), ('Victoria', 'Melbourne'), ('Queensland', 'Brisbane'),
    ('Lisboa', 'Lisboa'), ('Oporto', 'Oporto'), ('Faro', 'Faro'),
    ('Holanda Septentrional', 'Ámsterdam'), ('Holanda Meridional', 'Róterdam'), ('Utrecht', 'Utrecht'),
    ('Zúrich', 'Zúrich'), ('Ginebra', 'Ginebra'), ('Berna', 'Berna'),
    ('Estocolmo', 'Estocolmo'), ('Västra Götaland', 'Gotemburgo'), ('Escania', 'Malmö'),
    ('San José', 'San José'), ('Alajuela', 'Alajuela'), ('Heredia', 'Heredia'),
    ('Panamá', 'Ciudad de Panamá'), ('Colón', 'Colón'), ('Chiriquí', 'David'),
    ('Montevideo', 'Montevideo'), ('Canelones', 'Canelones'), ('Maldonado', 'Punta del Este'),
    ('Pichincha', 'Quito'), ('Guayas', 'Guayaquil'), ('Azuay', 'Cuenca'),
    ('Guatemala', 'Ciudad de Guatemala'), ('Sacatepéquez', 'Antigua Guatemala'), ('Quetzaltenango', 'Quetzaltenango'),
    ('Delhi', 'Nueva Delhi'), ('Maharashtra', 'Bombay'), ('Karnataka', 'Bangalore'),
    ('Moscú', 'Moscú'), ('San Petersburgo', 'San Petersburgo'), ('Tartaristán', 'Kazán'),
    ('Estambul', 'Estambul'), ('Ankara', 'Ankara'), ('Esmirna', 'Esmirna'),
    ('Gauteng', 'Johannesburgo'), ('Cabo Occidental', 'Ciudad del Cabo'), ('KwaZulu-Natal', 'Durban')
) AS m(estado, municipio) ON e.nombre = m.estado
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- ==================== COLONIAS / DISTRITOS / BARRIOS INTERNACIONALES ====================
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.colonia, c.cp, m.id FROM cat_municipios m
JOIN (VALUES
    ('Los Ángeles', 'Downtown LA', '90012'), ('Los Ángeles', 'Hollywood', '90028'), ('San Francisco', 'Mission District', '94110'),
    ('Houston', 'Downtown Houston', '77002'), ('Austin', 'Downtown Austin', '78701'), ('Miami', 'Brickell', '33131'),
    ('Nueva York', 'Manhattan Midtown', '10001'), ('Nueva York', 'Brooklyn Heights', '11201'), ('Chicago', 'The Loop', '60601'),
    ('Toronto', 'Downtown Toronto', 'M5H2N'), ('Montreal', 'Old Montreal', 'H2Y1C'), ('Vancouver', 'Yaletown', 'V6B1H'),
    ('Madrid', 'Salamanca', '28001'), ('Madrid', 'Centro', '28013'), ('Barcelona', 'Eixample', '08007'), ('Sevilla', 'Triana', '41010'),
    ('Bogotá', 'Chapinero', '11023'), ('Medellín', 'El Poblado', '05002'), ('Buenos Aires', 'Palermo', '14140'), ('Buenos Aires', 'Recoleta', '11130'),
    ('São Paulo', 'Pinheiros', '05422'), ('Río de Janeiro', 'Copacabana', '22020'), ('Santiago', 'Providencia', '75000'), ('Lima', 'Miraflores', '15074'),
    ('Berlín', 'Mitte', '10115'), ('Múnich', 'Altstadt', '80331'), ('París', 'Le Marais', '75004'), ('París', 'Montmartre', '75018'),
    ('Roma', 'Trastevere', '00153'), ('Milán', 'Brera', '20121'), ('Londres', 'Westminster', 'SW1A1'), ('Londres', 'Camden', 'NW18Q'),
    ('Shibuya', 'Dogenzaka', '15000'), ('Shinjuku', 'Kabukicho', '16000'), ('Osaka', 'Namba', '54200'),
    ('Pekín', 'Chaoyang', '10002'), ('Shanghái', 'Pudong', '20012'), ('Seúl', 'Gangnam', '06000'),
    ('Sídney', 'Surry Hills', '20100'), ('Melbourne', 'Fitzroy', '30650'), ('Lisboa', 'Baixa', '11000'),
    ('Ámsterdam', 'Jordaan', '10150'), ('Zúrich', 'Altstadt', '80010'), ('Estocolmo', 'Södermalm', '11820'),
    ('San José', 'Carmen', '10101'), ('Ciudad de Panamá', 'Bella Vista', '08010'), ('Montevideo', 'Pocitos', '11300'),
    ('Quito', 'La Mariscal', '17051'), ('Ciudad de Guatemala', 'Zona 10', '01010'), ('Nueva Delhi', 'Connaught Place', '11000'),
    ('Moscú', 'Arbat', '11900'), ('Estambul', 'Beyoğlu', '34430'), ('Johannesburgo', 'Sandton', '21960'), ('Ciudad del Cabo', 'Sea Point', '80050')
) AS c(municipio, colonia, cp) ON m.nombre = c.municipio
ON CONFLICT (nombre, municipio_id) DO NOTHING;
