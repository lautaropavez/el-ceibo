-- V1: baseline del esquema ACTUAL de libreriaweb.
-- Fotografia exacta del esquema real auditado en P12.5.7, tomada de
-- SHOW CREATE TABLE. No incluye ningun cambio futuro (isbn sigue
-- bigint, portada_id sigue sin UNIQUE, prestamo sin cambios).
-- Orden de creacion respeta dependencias de FK:
--   autor, editorial, portada, usuario (sin FK)
--   libro (FK -> autor, editorial, portada)
--   prestamo (FK -> libro, usuario)

CREATE TABLE `autor` (
  `id` varchar(255) NOT NULL,
  `alta` bit(1) NOT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_hrxnpr6qgsqoctag1ckjq2dcs` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `editorial` (
  `id` varchar(255) NOT NULL,
  `alta` bit(1) NOT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_eapnhl7c4en8ug7jfqahnyn8f` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `portada` (
  `id` varchar(255) NOT NULL,
  `contenido` longblob DEFAULT NULL,
  `mime` varchar(255) DEFAULT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `usuario` (
  `id` varchar(255) NOT NULL,
  `alta` datetime DEFAULT NULL,
  `apellido` varchar(255) NOT NULL,
  `baja` datetime DEFAULT NULL,
  `cant_prestamos` int(11) DEFAULT NULL,
  `clave` varchar(255) NOT NULL,
  `mail` varchar(255) DEFAULT NULL,
  `nombre` varchar(255) NOT NULL,
  `rol` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ics8o4kc1rvjcm04kqcj4mr9b` (`mail`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `libro` (
  `id` varchar(255) NOT NULL,
  `alta` bit(1) DEFAULT NULL,
  `anio` int(11) DEFAULT NULL,
  `ejemplares_prestados` int(11) DEFAULT NULL,
  `ejemplares_restantes` int(11) DEFAULT NULL,
  `isbn` bigint(20) DEFAULT NULL,
  `nroejemplares` int(11) DEFAULT NULL,
  `titulo` varchar(255) NOT NULL,
  `autor_id` varchar(255) DEFAULT NULL,
  `editorial_id` varchar(255) DEFAULT NULL,
  `portada_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ehuya6b4bxgkc4ru5wcf5njgr` (`isbn`),
  KEY `FKe1ss87ymon6qj17bhr6jfh0c4` (`autor_id`),
  KEY `FK79q7g2604hcmfdxw6ek3jt4el` (`editorial_id`),
  KEY `FK2ua832tbhe5eokdhaftrfpf24` (`portada_id`),
  CONSTRAINT `FK2ua832tbhe5eokdhaftrfpf24` FOREIGN KEY (`portada_id`) REFERENCES `portada` (`id`),
  CONSTRAINT `FK79q7g2604hcmfdxw6ek3jt4el` FOREIGN KEY (`editorial_id`) REFERENCES `editorial` (`id`),
  CONSTRAINT `FKe1ss87ymon6qj17bhr6jfh0c4` FOREIGN KEY (`autor_id`) REFERENCES `autor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `prestamo` (
  `id` varchar(36) NOT NULL,
  `alta` bit(1) DEFAULT NULL,
  `devolucion` datetime DEFAULT NULL,
  `prestamo` datetime DEFAULT NULL,
  `libro_id` varchar(255) DEFAULT NULL,
  `usuario_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKk7uwwn2ov4su2plcn1jh4dbi0` (`libro_id`),
  KEY `FKqxhq6d4w6fuv27c7j3af28wdu` (`usuario_id`),
  CONSTRAINT `FKk7uwwn2ov4su2plcn1jh4dbi0` FOREIGN KEY (`libro_id`) REFERENCES `libro` (`id`),
  CONSTRAINT `FKqxhq6d4w6fuv27c7j3af28wdu` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
