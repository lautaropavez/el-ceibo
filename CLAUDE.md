Workflow por postas



Secuencia estándar:



auditoría / comprensión;

decisiones;

implementación;

STOP — revisión de diff;

pruebas funcionales;

STOP — aprobación;

commit;

STOP — control acumulado;

merge solo con aprobación.



No repetir auditorías ya cerradas salvo que el diff real revele

un problema nuevo.



Checkpoints



Antes de editar:



verificar rama, HEAD, status y alcance.



Después de implementar:



compilar;

git diff --check;

revisar archivos modificados;

STOP antes de pruebas.



Después de pruebas:



entregar matriz/resultados;

confirmar que el diff no cambió;

STOP antes de commit.



Después de commit:



STOP antes de merge.



Antes de merge:



control acumulado de branch vs master.

Git

Nunca usar git add ..

Stagear archivos explícitos.

Nunca agregar Co-Authored-By.

No push salvo pedido explícito.

No merge salvo aprobación explícita.

No reset/clean/rebase destructivo sin aprobación.

Los dos .txt manuales del repo no deben stagearse:

AVANCES El Ceibo.txt

ROADMAP EL CEIBO — VERSIÓN ACTUAL.txt

Entorno

Proyecto Java 8.

Perfil normal: local.

No usar perfil demo salvo pedido explícito.

No imprimir ni modificar configuración local sensible.

No exponer credenciales/secrets.

Bases y pruebas

libreriaweb es la base normal.

Pruebas destructivas o con datos artificiales deben realizarse

en una base temporal dedicada.

Preferir un clon fresco para cada matriz principal.

Bases temporales viejas no se reutilizan como baseline si ya

están contaminadas con casos legacy.

Nunca DROP de una base sin aprobación explícita.

No ejecutar SQL destructivo fuera de una base temporal

explícitamente autorizada.

Alcance



Cada posta define:



objetivo;

archivos permitidos;

invariantes;

fuera de alcance;

pruebas.



No tocar archivos o áreas fuera del alcance sin detenerse y

reportar primero.



Filosofía de cambios

Cambios mínimos y trazables.

No refactorizar por gusto mientras se corrige otra cosa.

No construir infraestructura permanente para tests

descartables.

Los tests permanentes se incorporarán en la etapa específica

del roadmap.

Si aparece un hallazgo nuevo concreto durante implementación o

pruebas, resolver ese hallazgo; no reiniciar toda la auditoría.



Migraciones y esquema



Flyway administra el esquema de la base.

Hibernate usa ddl-auto=validate.

No volver a ddl-auto=update/create.

Las migraciones ya aplicadas son inmutables.

Nunca editar V1, V2 o V3.

Todo cambio futuro de esquema debe realizarse mediante una

nueva migración versionada.

No persistir spring.flyway.baseline-on-migrate=true.

El baseline fue únicamente parte de la adopción inicial controlada.

No modificar flyway\_schema\_history manualmente.





Proporcionalidad de pruebas



La intensidad de las pruebas depende del riesgo del cambio.

Cambios pequeños y de bajo riesgo:

build + diff + chequeo estático.



Las pruebas funcionales pueden agruparse con la siguiente matriz

real relacionada.



Cambios medianos:

pruebas funcionales acotadas al flujo afectado.



Cambios de seguridad, base de datos, migraciones, concurrencia,

archivos, permisos o acciones destructivas:



matriz dedicada antes del commit.

No repetir matrices ya cerradas sin una causa concreta.





Agentes



Los agentes especializados complementan este workflow;

no reemplazan las reglas de seguridad, Git, alcance ni checkpoints.



El implementer puede modificar únicamente archivos dentro del

scope explícito de la posta.



El reviewer es independiente del implementer y debe operar

en modo read-only. No puede editar código ni corregir sus propios

hallazgos.



El tester debe buscar fallos de manera adversarial, no limitarse

a confirmar el happy path.



Findings de reviewer/tester vuelven al implementer para corrección.



Tester y reviewer no pueden:

\- mergear;

\- pushear;

\- stagear;

\- commitear;

\- tocar libreriaweb normal;

\- modificar secrets/config local;

\- ampliar el scope por decisión propia.



Las decisiones nuevas de producto vuelven al humano.



No usar autonomía multiagente para tareas triviales si agrega

más complejidad que valor.