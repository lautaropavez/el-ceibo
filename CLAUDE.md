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
