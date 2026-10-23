// Menú lateral: botón accesible (aria-expanded), cierre con Escape y devolución
// del foco al botón. En pantallas chicas funciona como panel superpuesto.
(function () {
    const MOBILE_QUERY = "(max-width: 767.98px)";

    const body = document.getElementById("body");
    const sideMenu = document.getElementById("menu_side");
    const btnOpen = document.getElementById("btn_open");
    if (!body || !sideMenu || !btnOpen) {
        return;
    }

    const mobile = window.matchMedia(MOBILE_QUERY);
    const backdrop = document.createElement("div");
    backdrop.className = "ceibo-sidebar-backdrop";
    body.appendChild(backdrop);

    function isOpen() {
        return sideMenu.classList.contains("menu__side_move");
    }

    function render(open) {
        body.classList.toggle("body_move", open);
        sideMenu.classList.toggle("menu__side_move", open);
        backdrop.classList.toggle("is-visible", open && mobile.matches);
        btnOpen.setAttribute("aria-expanded", String(open));
        btnOpen.setAttribute("aria-label", open ? "Cerrar el menú de gestión" : "Abrir el menú de gestión");
    }

    function open() {
        render(true);
        if (mobile.matches) {
            const firstLink = sideMenu.querySelector(".options__menu a, .options__menu button");
            if (firstLink) {
                firstLink.focus();
            }
        }
    }

    function close(returnFocus) {
        render(false);
        if (returnFocus) {
            btnOpen.focus();
        }
    }

    btnOpen.addEventListener("click", function () {
        if (isOpen()) {
            close(false);
        } else {
            open();
        }
    });

    backdrop.addEventListener("click", function () {
        close(true);
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && isOpen()) {
            close(true);
        }
    });

    // Al cruzar el punto de corte se vuelve al estado cerrado.
    const onBreakpointChange = function () {
        render(false);
    };
    if (mobile.addEventListener) {
        mobile.addEventListener("change", onBreakpointChange);
    } else if (mobile.addListener) {
        mobile.addListener(onBreakpointChange);
    }

    render(false);
})();
