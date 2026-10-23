"use strict";
(() => {
    const sidebar = document.getElementById("adminSidebar");
    const trigger = document.getElementById("menuAdmin");
    const backdrop = document.getElementById("adminBackdrop");
    if (trigger && sidebar && backdrop) {
        const toggle = (open) => {
            sidebar.classList.toggle("sidebar-open", open);
            backdrop.classList.toggle("visible", open);
            trigger.setAttribute("aria-expanded", String(open));
        };
        trigger.addEventListener("click", () => toggle(!sidebar.classList.contains("sidebar-open")));
        backdrop.addEventListener("click", () => toggle(false));
        document.addEventListener("keydown", (event) => { if (event.key === "Escape") toggle(false); });
    }
    // En lugar de confirm() del navegador, mostramos el mismo modal Bootstrap en todo el panel.
    const modalElement = document.getElementById("gatrixConfirmModal");
    const confirmButton = document.getElementById("gatrixConfirmAccept");
    let pendingForm = null;
    if (modalElement && confirmButton && window.bootstrap) {
        const confirmModal = new bootstrap.Modal(modalElement);
        document.addEventListener("submit", (event) => {
            const form = event.target.closest("form[data-confirm]");
            if (!form || form.dataset.confirmed === "true") return;
            if (!form.reportValidity()) return;
            event.preventDefault();
            pendingForm = form;
            document.getElementById("gatrixConfirmMessage").textContent =
                form.dataset.confirm || "¿Confirmas esta operación?";
            confirmModal.show();
        });
        confirmButton.addEventListener("click", () => {
            if (!pendingForm) return;
            const form = pendingForm;
            pendingForm = null;
            form.dataset.confirmed = "true";
            confirmModal.hide();
            form.requestSubmit();
        });
        modalElement.addEventListener("hidden.bs.modal", () => { pendingForm = null; });
    }
    const pathname = window.location.pathname;
    document.querySelectorAll(".sidebar-links a").forEach((link) => {
        const target = new URL(link.href).pathname;
        const matches = pathname === target || (target !== "/admin" && pathname.startsWith(target + "/"));
        if (matches) { link.classList.add("selected"); link.setAttribute("aria-current", "page"); }
    });
})();
