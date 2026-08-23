package com.mza.Libreria.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Agrupa la configuracion estandar de un test de integracion contra
 * libreriaweb_testsuite. No incluye @Transactional: ServicePrestamo (y
 * cualquier otro service probado con esta anotacion) controla sus propias
 * transacciones reales, igual que en produccion. Una clase futura solo
 * agrega @Transactional de forma explicita en si misma si existe una razon
 * demostrada para esa clase puntual.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseInitializer.class)
public @interface LibreriaIntegrationTest {
}
