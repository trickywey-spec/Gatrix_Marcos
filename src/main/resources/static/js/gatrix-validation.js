/* GATRIX CarWash · validación accesible en tiempo real (complementa, no sustituye, la validación de Spring). */
"use strict";
(() => {
  const FIELD_SELECTOR = 'input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"]):not([type="submit"]), textarea, select';
  const onlyDigits = (value) => value.replace(/\D/g, "");
  const rules = {
    name: {
      clean: (v) => v.replace(/[^\p{L}\s]/gu, "").replace(/ {2,}/g, " ").replace(/^\s+/, ""),
      test: (v) => /^[\p{L}]+(?: [\p{L}]+)*$/u.test(v) && v.length >= 2,
      message: "Ingresa al menos 2 letras. Utiliza solo letras y espacios."
    },
    phone: {
      clean: (v) => onlyDigits(v).slice(0, 9),
      test: (v) => /^9\d{8}$/.test(v),
      message: "Ingresa un celular peruano de 9 dígitos, comenzando por 9."
    },
    plate: {
      clean: (v) => v.toUpperCase().replace(/[^A-Z0-9-]/g, "").slice(0, 20),
      test: (v) => /^[A-Z0-9]+(?:-[A-Z0-9]+)*$/.test(v) && v.length >= 4 && v.length <= 20,
      message: "Usa letras, números y guiones (ej.: ABC-123)."
    },
    code: {
      clean: (v) => v.toUpperCase().replace(/[^A-Z0-9-]/g, "").slice(0, 40),
      test: (v) => /^[A-Z0-9]+(?:-[A-Z0-9]+)*$/.test(v) && v.length >= 5,
      message: "Ingresa el código de reserva, sin espacios ni símbolos."
    },
    username: {
      clean: (v) => v.replace(/[^a-zA-Z0-9_.-]/g, ""),
      test: (v) => /^[a-zA-Z0-9_.-]{3,50}$/.test(v),
      message: "Utiliza entre 3 y 50 letras, números, puntos, guiones o guion bajo."
    },
    integer: {
      clean: (v) => onlyDigits(v),
      test: (v) => /^\d+$/.test(v),
      message: "Escribe únicamente números enteros."
    },
    decimal: {
      clean: (v) => v.replace(/,/g, ".").replace(/[^\d.]/g, "").replace(/(\..*)\./g, "$1"),
      test: (v) => /^\d+(?:\.\d{1,2})?$/.test(v),
      message: "Usa un importe numérico válido (máximo 2 decimales)."
    },
    email: {
      clean: (v) => v.replace(/\s/g, ""),
      test: (v) => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v),
      message: "Escribe un correo válido, por ejemplo nombre@correo.com."
    }
  };

  function labelFor(field) {
    const label = field.id ? document.querySelector(`label[for="${CSS.escape(field.id)}"]`) : null;
    return label?.textContent?.replace(/\*/g, "").trim() || field.getAttribute("aria-label") || "Este campo";
  }
  function feedbackAnchor(field) {
    return field.closest(".input-group") || field;
  }
  function messageFor(field, text) {
    const anchor = feedbackAnchor(field);
    let feedback = anchor.nextElementSibling;
    if (!feedback?.classList.contains("gatrix-feedback")) {
      feedback = document.createElement("div");
      feedback.className = "gatrix-feedback";
      feedback.id = `gatrix-help-${Math.random().toString(36).slice(2, 10)}`;
      anchor.insertAdjacentElement("afterend", feedback);
    }
    feedback.textContent = text;
    feedback.hidden = !text;
    field.setAttribute("aria-describedby", feedback.id);
  }
  function resetMessage(field) {
    field.classList.remove("gatrix-field-valid", "gatrix-field-invalid");
    field.removeAttribute("aria-invalid");
    field.setCustomValidity("");
  }
  function status(field, show = true) {
    const kind = field.dataset.validate;
    const rule = rules[kind];
    const raw = field.value || "";
    field.setCustomValidity("");
    const isOptionalEmpty = !field.required && !raw.trim();
    if (isOptionalEmpty) {
      resetMessage(field);
      const existing = feedbackAnchor(field).nextElementSibling;
      if (existing?.classList.contains("gatrix-feedback")) existing.hidden = true;
      return true;
    }
    const formattedValid = !rule || rule.test(raw.trim());
    const nativeValid = field.checkValidity();
    const requiredMissing = field.required && !raw.trim();
    const valid = !requiredMissing && formattedValid && nativeValid;
    if (!formattedValid) field.setCustomValidity(rule.message);
    if (!show) return valid;
    field.classList.toggle("gatrix-field-valid", valid);
    field.classList.toggle("gatrix-field-invalid", !valid);
    field.setAttribute("aria-invalid", String(!valid));
    if (valid) {
      const existing = feedbackAnchor(field).nextElementSibling;
      if (existing?.classList.contains("gatrix-feedback")) existing.hidden = true;
    } else {
      const text = !raw.trim() ? `${labelFor(field)} es obligatorio.` :
        !formattedValid ? rule.message : field.validity.rangeUnderflow ? `El mínimo permitido es ${field.min}.` :
        field.validity.rangeOverflow ? `El máximo permitido es ${field.max}.` :
        field.validity.tooShort ? `Escribe al menos ${field.minLength} caracteres.` :
        field.validity.typeMismatch ? "Comprueba el formato de este campo." : "Revisa este dato antes de continuar.";
      messageFor(field, text);
    }
    return valid;
  }
  const allForms = [...document.querySelectorAll("form")];
  allForms.forEach((form) => {
    const fields = [...form.querySelectorAll(FIELD_SELECTOR)].filter((f) =>
      (!f.disabled || f.id === "hora") && (f.required || Boolean(f.dataset.validate) || f.type === "number")
    );
    if (!fields.length) return;
    form.noValidate = true; // La validación sigue activa, pero mostramos nuestros mensajes antes del envío.
    for (const field of fields) {
      const rule = rules[field.dataset.validate];
      if (field.dataset.validate === "phone") field.setAttribute("inputmode", "numeric");
      if (field.type === "number") {
        field.addEventListener("keydown", (e) => {
          if (["e", "E", "+"].includes(e.key) || (e.key === "-" && Number(field.min) >= 0)) e.preventDefault();
        });
      }
      field.addEventListener("input", () => {
        if (rule && !["number", "date", "time"].includes(field.type)) {
          const cleaned = rule.clean(field.value);
          if (cleaned !== field.value) field.value = cleaned;
        }
        field.dataset.gatrixTouched = "yes";
        status(field);
      });
      field.addEventListener("change", () => { field.dataset.gatrixTouched = "yes"; status(field); });
      field.addEventListener("blur", () => {
        if (field.dataset.validate === "name") field.value = field.value.trim().replace(/\s+/g, " ");
        field.dataset.gatrixTouched = "yes";
        status(field);
      });
    }
  });

  function statusServices(form) {
    const grid = form.querySelector("#bookingServices");
    if (!grid) return true;
    const boxes = [...grid.querySelectorAll('input[type="checkbox"][name="servicioIds"]')];
    const valid = boxes.some((box) => box.checked);
    grid.classList.toggle("gatrix-group-invalid", !valid);
    grid.classList.toggle("gatrix-group-valid", valid);
    let help = grid.nextElementSibling;
    if (!help?.classList.contains("gatrix-group-feedback")) {
      help = document.createElement("div");
      help.className = "gatrix-feedback gatrix-group-feedback";
      grid.insertAdjacentElement("afterend", help);
    }
    help.textContent = "Selecciona al menos un servicio para continuar.";
    help.hidden = valid;
    return valid;
  }
  const bookingGrid = document.getElementById("bookingServices");
  bookingGrid?.addEventListener("change", () => statusServices(bookingGrid.closest("form")));

  // Capture: validamos ANTES del listener que deshabilita el botón y envía la reserva.
  document.addEventListener("submit", (event) => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) return;
    const fields = [...form.querySelectorAll(FIELD_SELECTOR)].filter((f) =>
      (!f.disabled || f.id === "hora") && (f.required || Boolean(f.dataset.validate) || f.type === "number")
    );
    let firstInvalid = null;
    for (const field of fields) {
      if (!status(field)) firstInvalid ||= field;
    }
    const serviceValid = statusServices(form);
    if (!serviceValid) firstInvalid ||= form.querySelector("#bookingServices");
    if (firstInvalid) {
      event.preventDefault();
      event.stopImmediatePropagation();
      firstInvalid.scrollIntoView({ behavior: "smooth", block: "center" });
      if (typeof firstInvalid.focus === "function") firstInvalid.focus({ preventScroll: true });
    }
  }, true);

  // La pestaña activa depende de la URL, no del HTML: misma barra en todas las páginas.
  const route = location.pathname.replace(/\/$/, "") || "/index.html";
  document.querySelectorAll(".gatrix-menu .nav-link").forEach((link) => {
    const path = link.getAttribute("href")?.split("?")[0] || "";
    const active = path === route || (route === "/" && path === "/index.html") ||
      (route === "/consulta.html" && path === "/consulta");
    link.classList.toggle("active", active);
    if (active) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
})();
