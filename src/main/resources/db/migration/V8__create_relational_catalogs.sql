-- V8: Catálogos relacionales (Ocupaciones, Países -> Estados -> Municipios -> Colonias)
-- y hacer ocupacion opcional en clientes cuando no se ingresa empresa

ALTER TABLE clientes ALTER COLUMN ocupacion DROP NOT NULL;

CREATE TABLE IF NOT EXISTS cat_ocupaciones (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cat_paises (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cat_estados (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    pais_id INTEGER NOT NULL REFERENCES cat_paises(id) ON DELETE CASCADE,
    UNIQUE(nombre, pais_id)
);

CREATE TABLE IF NOT EXISTS cat_municipios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    estado_id INTEGER NOT NULL REFERENCES cat_estados(id) ON DELETE CASCADE,
    UNIQUE(nombre, estado_id)
);

CREATE TABLE IF NOT EXISTS cat_colonias (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo_postal VARCHAR(10),
    municipio_id INTEGER NOT NULL REFERENCES cat_municipios(id) ON DELETE CASCADE,
    UNIQUE(nombre, municipio_id)
);

CREATE INDEX IF NOT EXISTS idx_cat_estados_pais ON cat_estados(pais_id);
CREATE INDEX IF NOT EXISTS idx_cat_municipios_estado ON cat_municipios(estado_id);
CREATE INDEX IF NOT EXISTS idx_cat_colonias_municipio ON cat_colonias(municipio_id);

-- ==================== SEMILLA: OCUPACIONES ====================
INSERT INTO cat_ocupaciones (nombre) VALUES
    ('Ingeniería y Desarrollo de Software'),
    ('Medicina y Servicios de Salud'),
    ('Docencia y Educación'),
    ('Contabilidad y Auditoría'),
    ('Administración y Gestión Empresarial'),
    ('Comercio y Ventas'),
    ('Derecho y Servicios Jurídicos'),
    ('Arquitectura y Construcción'),
    ('Estudiante'),
    ('Empleado Administrativo'),
    ('Transporte y Logística'),
    ('Hostelería y Turismo'),
    ('Arte y Diseño Digital'),
    ('Finanzas y Banca'),
    ('Independiente / Emprendedor'),
    ('Sector Agropecuario e Industrial'),
    ('Servicios Generales y Mantenimiento'),
    ('Investigación Científica'),
    ('Otra')
ON CONFLICT (nombre) DO NOTHING;

-- ==================== SEMILLA: 30 PAÍSES + OTRO ====================
INSERT INTO cat_paises (nombre) VALUES
    ('México'),
    ('Estados Unidos'),
    ('Canadá'),
    ('España'),
    ('Argentina'),
    ('Colombia'),
    ('Brasil'),
    ('Chile'),
    ('Perú'),
    ('Alemania'),
    ('Francia'),
    ('Italia'),
    ('Reino Unido'),
    ('Japón'),
    ('China'),
    ('Corea del Sur'),
    ('Australia'),
    ('Portugal'),
    ('Países Bajos'),
    ('Suiza'),
    ('Suecia'),
    ('Costa Rica'),
    ('Panamá'),
    ('Uruguay'),
    ('Ecuador'),
    ('Guatemala'),
    ('India'),
    ('Rusia'),
    ('Turquía'),
    ('Sudáfrica'),
    ('Otro')
ON CONFLICT (nombre) DO NOTHING;

-- ==================== SEMILLA: ESTADOS POR PAÍS ====================
-- México (32 entidades)
INSERT INTO cat_estados (nombre, pais_id)
SELECT e.nombre, p.id FROM cat_paises p
CROSS JOIN (VALUES
    ('Aguascalientes'), ('Baja California'), ('Baja California Sur'), ('Campeche'),
    ('Chiapas'), ('Chihuahua'), ('Ciudad de México'), ('Coahuila'),
    ('Colima'), ('Durango'), ('Estado de México'), ('Guanajuato'),
    ('Guerrero'), ('Hidalgo'), ('Jalisco'), ('Michoacán'),
    ('Morelos'), ('Nayarit'), ('Nuevo León'), ('Oaxaca'),
    ('Puebla'), ('Querétaro'), ('Quintana Roo'), ('San Luis Potosí'),
    ('Sinaloa'), ('Sonora'), ('Tabasco'), ('Tamaulipas'),
    ('Tlaxcala'), ('Veracruz'), ('Yucatán'), ('Zacatecas')
) AS e(nombre)
WHERE p.nombre = 'México'
ON CONFLICT (nombre, pais_id) DO NOTHING;

-- Estados Unidos
INSERT INTO cat_estados (nombre, pais_id)
SELECT e.nombre, p.id FROM cat_paises p
CROSS JOIN (VALUES
    ('California'), ('Texas'), ('Florida'), ('Nueva York'), ('Illinois'), ('Arizona'), ('Nevada')
) AS e(nombre)
WHERE p.nombre = 'Estados Unidos'
ON CONFLICT (nombre, pais_id) DO NOTHING;

-- Canadá
INSERT INTO cat_estados (nombre, pais_id)
SELECT e.nombre, p.id FROM cat_paises p
CROSS JOIN (VALUES
    ('Ontario'), ('Quebec'), ('Columbia Británica'), ('Alberta')
) AS e(nombre)
WHERE p.nombre = 'Canadá'
ON CONFLICT (nombre, pais_id) DO NOTHING;

-- España
INSERT INTO cat_estados (nombre, pais_id)
SELECT e.nombre, p.id FROM cat_paises p
CROSS JOIN (VALUES
    ('Comunidad de Madrid'), ('Cataluña'), ('Andalucía'), ('Comunidad Valenciana'), ('País Vasco')
) AS e(nombre)
WHERE p.nombre = 'España'
ON CONFLICT (nombre, pais_id) DO NOTHING;

-- Colombia
INSERT INTO cat_estados (nombre, pais_id)
SELECT e.nombre, p.id FROM cat_paises p
CROSS JOIN (VALUES
    ('Bogotá D.C.'), ('Antioquia'), ('Valle del Cauca'), ('Atlántico')
) AS e(nombre)
WHERE p.nombre = 'Colombia'
ON CONFLICT (nombre, pais_id) DO NOTHING;

-- Argentina
INSERT INTO cat_estados (nombre, pais_id)
SELECT e.nombre, p.id FROM cat_paises p
CROSS JOIN (VALUES
    ('Buenos Aires'), ('Ciudad Autónoma de Buenos Aires'), ('Córdoba'), ('Santa Fe'), ('Mendoza')
) AS e(nombre)
WHERE p.nombre = 'Argentina'
ON CONFLICT (nombre, pais_id) DO NOTHING;

-- ==================== SEMILLA: MUNICIPIOS / ALCALDÍAS POR ESTADO ====================
-- Guanajuato
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES
    ('Dolores Hidalgo'), ('San Miguel de Allende'), ('Guanajuato'), ('León'),
    ('Celaya'), ('Irapuato'), ('Salamanca'), ('Silao'),
    ('San Luis de la Paz'), ('San Diego de la Unión'), ('San Felipe'),
    ('Comonfort'), ('Cortazar'), ('Acámbaro'), ('Valle de Santiago')
) AS m(nombre)
WHERE e.nombre = 'Guanajuato'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- Ciudad de México
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES
    ('Cuauhtémoc'), ('Benito Juárez'), ('Miguel Hidalgo'), ('Coyoacán'),
    ('Álvaro Obregón'), ('Iztapalapa'), ('Gustavo A. Madero'), ('Tlalpan'), ('Azcapotzalco')
) AS m(nombre)
WHERE e.nombre = 'Ciudad de México'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- Jalisco
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES
    ('Guadalajara'), ('Zapopan'), ('San Pedro Tlaquepaque'), ('Tonalá'), ('Puerto Vallarta'), ('Tlajomulco de Zúñiga')
) AS m(nombre)
WHERE e.nombre = 'Jalisco'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- Nuevo León
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES
    ('Monterrey'), ('San Pedro Garza García'), ('Guadalupe'), ('Apodaca'), ('San Nicolás de los Garza'), ('Santa Catarina')
) AS m(nombre)
WHERE e.nombre = 'Nuevo León'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- Querétaro
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES
    ('Querétaro'), ('San Juan del Río'), ('El Marqués'), ('Corregidora'), ('Tequisquiapan')
) AS m(nombre)
WHERE e.nombre = 'Querétaro'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- Estado de México
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES
    ('Toluca'), ('Ecatepec de Morelos'), ('Naucalpan de Juárez'), ('Tlalnepantla de Baz'), ('Nezahualcóyotl'), ('Metepec')
) AS m(nombre)
WHERE e.nombre = 'Estado de México'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- Puebla
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES
    ('Puebla'), ('San Andrés Cholula'), ('Tehuacán'), ('Atlixco')
) AS m(nombre)
WHERE e.nombre = 'Puebla'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- Aguascalientes, San Luis Potosí, Michoacán, Yucatán, Quintana Roo, Veracruz
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES ('Aguascalientes'), ('Jesús María'), ('Calvillo')) AS m(nombre)
WHERE e.nombre = 'Aguascalientes'
ON CONFLICT (nombre, estado_id) DO NOTHING;

INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES ('San Luis Potosí'), ('Soledad de Graciano Sánchez'), ('Ciudad Valles')) AS m(nombre)
WHERE e.nombre = 'San Luis Potosí'
ON CONFLICT (nombre, estado_id) DO NOTHING;

INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES ('Morelia'), ('Uruapan'), ('Zamora')) AS m(nombre)
WHERE e.nombre = 'Michoacán'
ON CONFLICT (nombre, estado_id) DO NOTHING;

INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES ('Mérida'), ('Progreso'), ('Valladolid')) AS m(nombre)
WHERE e.nombre = 'Yucatán'
ON CONFLICT (nombre, estado_id) DO NOTHING;

INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES ('Benito Juárez (Cancún)'), ('Solidaridad (Playa del Carmen)'), ('Othón P. Blanco')) AS m(nombre)
WHERE e.nombre = 'Quintana Roo'
ON CONFLICT (nombre, estado_id) DO NOTHING;

INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'México'
CROSS JOIN (VALUES ('Veracruz'), ('Xalapa'), ('Boca del Río'), ('Coatzacoalcos')) AS m(nombre)
WHERE e.nombre = 'Veracruz'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- Estados Unidos (muestra)
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'Estados Unidos'
CROSS JOIN (VALUES ('Los Ángeles'), ('San Francisco'), ('San Diego'), ('San José')) AS m(nombre)
WHERE e.nombre = 'California'
ON CONFLICT (nombre, estado_id) DO NOTHING;

INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'Estados Unidos'
CROSS JOIN (VALUES ('Houston'), ('Austin'), ('Dallas'), ('San Antonio')) AS m(nombre)
WHERE e.nombre = 'Texas'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- España (muestra)
INSERT INTO cat_municipios (nombre, estado_id)
SELECT m.nombre, e.id FROM cat_estados e
JOIN cat_paises p ON p.id = e.pais_id AND p.nombre = 'España'
CROSS JOIN (VALUES ('Madrid'), ('Alcalá de Henares'), ('Getafe')) AS m(nombre)
WHERE e.nombre = 'Comunidad de Madrid'
ON CONFLICT (nombre, estado_id) DO NOTHING;

-- ==================== SEMILLA: COLONIAS POR MUNICIPIO ====================
-- Dolores Hidalgo
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Centro', '37800'),
    ('Lindavista', '37800'),
    ('San Cristóbal', '37800'),
    ('Guanajuato', '37800'),
    ('Mariano Balleza', '37800'),
    ('Todos Santos', '37800'),
    ('Fraccionamiento Independencia', '37800'),
    ('Río Laja', '37800')
) AS c(nombre, cp)
WHERE m.nombre = 'Dolores Hidalgo'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- San Miguel de Allende
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Centro', '37700'),
    ('San Antonio', '37750'),
    ('Guadalupe', '37710'),
    ('Ignacio Ramírez', '37748'),
    ('Los Frailes', '37790')
) AS c(nombre, cp)
WHERE m.nombre = 'San Miguel de Allende'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- Guanajuato
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Centro', '36000'),
    ('Marfil', '36250'),
    ('Yerbabuena', '36259'),
    ('Noria Alta', '36050'),
    ('San Javier', '36020')
) AS c(nombre, cp)
WHERE m.nombre = 'Guanajuato'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- León
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Centro', '37000'),
    ('Jardines del Moral', '37160'),
    ('Valle del Campestre', '37150'),
    ('León Moderno', '37480'),
    ('San Isidro', '37510')
) AS c(nombre, cp)
WHERE m.nombre = 'León'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- Celaya
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Centro', '38000'),
    ('Alameda', '38050'),
    ('Las Fuentes', '38040'),
    ('Renacimiento', '38060')
) AS c(nombre, cp)
WHERE m.nombre = 'Celaya'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- Irapuato
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Centro', '36500'),
    ('Las Reynas', '36660'),
    ('Villas de Irapuato', '36670'),
    ('Moderna', '36690')
) AS c(nombre, cp)
WHERE m.nombre = 'Irapuato'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- Cuauhtémoc (CDMX)
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Roma Norte', '06700'),
    ('Condesa', '06140'),
    ('Juárez', '06600'),
    ('Centro Histórico', '06000'),
    ('Santa María la Ribera', '06400')
) AS c(nombre, cp)
WHERE m.nombre = 'Cuauhtémoc'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- Benito Juárez (CDMX)
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Del Valle Centro', '03100'),
    ('Narvarte Poniente', '03020'),
    ('Nápoles', '03810'),
    ('Portales Norte', '03303')
) AS c(nombre, cp)
WHERE m.nombre = 'Benito Juárez'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- Guadalajara
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Americana', '44160'),
    ('Providencia', '44630'),
    ('Centro', '44100'),
    ('Chapalita', '44500')
) AS c(nombre, cp)
WHERE m.nombre = 'Guadalajara'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- Monterrey
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Centro', '64000'),
    ('Obispado', '64060'),
    ('Cumbres', '64610'),
    ('Tecnológico', '64700')
) AS c(nombre, cp)
WHERE m.nombre = 'Monterrey'
ON CONFLICT (nombre, municipio_id) DO NOTHING;

-- Querétaro
INSERT INTO cat_colonias (nombre, codigo_postal, municipio_id)
SELECT c.nombre, c.cp, m.id FROM cat_municipios m
CROSS JOIN (VALUES
    ('Centro Histórico', '76000'),
    ('Juriquilla', '76230'),
    ('Álamos', '76160'),
    ('Milenio III', '76060')
) AS c(nombre, cp)
WHERE m.nombre = 'Querétaro'
ON CONFLICT (nombre, municipio_id) DO NOTHING;
