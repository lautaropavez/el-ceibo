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



Las reglas compartidas entre agentes (límite de subagentes simultáneos,

delegación explícita, independencia, formato de findings, límites de

reportes y logs) tienen como fuente canónica docs/AGENT\_CONTRACT.md,

aplicable a cualquier runtime de agentes (Claude Code, Codex u otro).

Los perfiles pueden repetir el núcleo mínimo de seguridad definido allí

cuando sea necesario para funcionar de forma autónoma. Toda contradicción

con el contrato requiere STOP humano.



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



Gate A — Implementación y review



Scope exacto autorizado antes de editar.



Verificación estática o compilación proporcional al riesgo del cambio.



El reviewer independiente recibe únicamente la SPEC/contrato activo, el

diff real y los archivos estrictamente necesarios. No recibe la

conclusión del implementador ni una lista de findings esperados.

Preguntas dirigidas solo se agregan después de su revisión abierta,

únicamente para riesgos concretos ya identificados.



El implementer puede aplicar una sola corrección automática dentro del

scope autorizado, exclusivamente para findings objetivos de severidad

LOW o MEDIUM. El reviewer nunca aplica correcciones ni stagea ni

commitea, conforme a la sección "Agentes" de este documento.



BLOCKER, HIGH, una decisión de producto o cualquier expansión de scope

requieren STOP humano. Un segundo ciclo de corrección también requiere

STOP humano.



El adversarial-tester se invoca únicamente ante riesgo concreto:

seguridad, concurrencia, migraciones, operaciones destructivas o un

threat model explícito. No se invoca por rutina.



Gate B — Operación destructiva



Toda operación destructiva (ejecución real de tests que tocan una base

de datos, migraciones, DROP/CREATE, u operaciones equivalentes) requiere

una autorización humana separada de los Gates A, C y D — nunca implícita

ni agrupada con otro gate.



Antes de ejecutar: preflight read-only, destino e impacto exactos

confirmados.



Una sola ejecución por autorización. Si falla: no reintentar, no reparar

automáticamente. Identificar la primera causa raíz y STOP.



Gates C y D (stage+commit condicional y pre-merge+merge condicional)

están definidos operativamente en .claude/skills/close-posta/SKILL.md.

Esta sección no duplica ese procedimiento; define únicamente la política

de los Gates A y B.
