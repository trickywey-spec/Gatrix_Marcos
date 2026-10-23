"use strict";
(() => {
  const form = document.getElementById("formReserva");
  if (!form) return;

  const date = document.getElementById("fecha");
  const time = document.getElementById("hora");
  const count = document.getElementById("cantidadVehiculosReserva");
  const estimate = document.getElementById("totalReserva");
  const container = document.getElementById("bookingServices");
  const catalogError = document.getElementById("catalogError");
  const catalogErrorText = document.getElementById("catalogErrorText");
  const retryCatalog = document.getElementById("retryCatalog");
  const submit = form.querySelector('button[type="submit"]');
  const previouslySelected = new Set([...document.querySelectorAll(".previous-service")]
    .map(input => input.value));
  const urlSelected = new URLSearchParams(window.location.search).get("servicios");
  if (urlSelected && /^[\d,]+$/.test(urlSelected)) {
    urlSelected.split(",").slice(0, 20).forEach(id => previouslySelected.add(id));
  }
  let servicesLoaded = false;

  const money = value => new Intl.NumberFormat("es-PE", {
    style: "currency", currency: "PEN",
  }).format(value);
  const currentOptions = () => [...form.querySelectorAll('input[name="servicioIds"]')];

  function refreshEstimate() {
    const quantity = Math.min(10, Math.max(1, Number.parseInt(count.value, 10) || 1));
    const price = currentOptions().filter(opt => opt.checked).reduce(
      (total, opt) => total + Number(opt.dataset.precio || 0), 0) * quantity;
    estimate.textContent = money(price);
    currentOptions().forEach(opt => opt.closest(".booking-service")
      ?.classList.toggle("is-selected", opt.checked));
  }

  function createService(item) {
    const label = document.createElement("label");
    label.className = "booking-service";
    const input = document.createElement("input");
    input.type = "checkbox";
    input.name = "servicioIds";
    input.value = String(item.id);
    input.dataset.precio = String(item.precio);
    input.checked = previouslySelected.has(input.value);
    const check = document.createElement("span");
    check.className = "service-checkbox";
    const checkIcon = document.createElement("i");
    checkIcon.className = "fa-solid fa-check";
    check.append(checkIcon);
    const name = document.createElement("span");
    name.className = "service-name";
    const heading = document.createElement("strong");
    heading.textContent = String(item.nombre ?? "Servicio");
    const description = document.createElement("small");
    description.textContent = String(item.descripcion ?? "");
    name.append(heading, description);
    const price = document.createElement("b");
    price.textContent = money(Number(item.precio));
    label.append(input, check, name, price);
    input.addEventListener("change", refreshEstimate);
    return label;
  }

  async function loadServices() {
    submit.disabled = true;
    servicesLoaded = false;
    container.setAttribute("aria-busy", "true");
    container.replaceChildren();
    catalogError.classList.add("d-none");
    const loading = document.createElement("p");
    loading.className = "text-muted small py-3 mb-0";
    loading.textContent = "Cargando servicios disponibles...";
    container.append(loading);
    try {
      const response = await fetch("/api/servicios", {
        headers: { "Accept": "application/json" },
        credentials: "same-origin", cache: "no-store",
      });
      if (!response.ok) throw new Error(`El servidor respondió ${response.status}`);
      const items = await response.json();
      if (!Array.isArray(items)) throw new Error("El catálogo recibió datos inválidos");
      container.replaceChildren();
      items.forEach(item => container.append(createService(item)));
      if (items.length === 0) {
        catalogErrorText.textContent = "No hay servicios publicados. Solicita al administrador activar los servicios.";
        catalogError.classList.remove("d-none");
      } else {
        servicesLoaded = true;
        submit.disabled = false;
      }
    } catch (error) {
      container.replaceChildren();
      catalogErrorText.textContent = "No se pudieron consultar los servicios desde PostgreSQL. " +
        "Revisa que la base de datos esté disponible y vuelve a intentar.";
      catalogError.classList.remove("d-none");
      console.error("GATRIX: error al consultar el catálogo", error);
    } finally {
      container.setAttribute("aria-busy", "false");
      refreshEstimate();
    }
  }

  retryCatalog?.addEventListener("click", loadServices);
  count.addEventListener("input", refreshEstimate);
  loadServices();

  let lastRequest = 0;
  async function loadSlots() {
    const requestId = ++lastRequest;
    const previous = time.value || time.dataset.selected || "";
    time.replaceChildren(new Option(date.value ? "Consultando horarios..." : "Selecciona una fecha", ""));
    time.disabled = true;
    if (!date.value) return;
    try {
      const response = await fetch(`/api/horarios?fecha=${encodeURIComponent(date.value)}`, {
        headers: { Accept: "application/json" }, credentials: "same-origin", cache: "no-store",
      });
      if (!response.ok) throw new Error(`Error ${response.status}`);
      const slots = await response.json();
      if (requestId !== lastRequest) return;
      time.replaceChildren(new Option("Selecciona un horario", ""));
      let available = 0;
      for (const slot of slots) {
        const option = new Option(`${String(slot.hora).substring(0, 5)} ${slot.disponible ? "· Disponible" : "· Ocupado"}`, slot.hora);
        option.disabled = !slot.disponible;
        time.add(option);
        if (slot.disponible) available++;
      }
      if (!available) time.options[0].textContent = "No hay horarios disponibles";
      if (slots.some(item => item.hora === previous && item.disponible)) time.value = previous;
      time.disabled = !available;
    } catch (error) {
      if (requestId !== lastRequest) return;
      time.replaceChildren(new Option("No se pudo consultar la agenda; reintenta", ""));
      time.disabled = true;
      console.error("GATRIX: error al consultar horarios", error);
    }
  }

  date.addEventListener("change", () => { time.dataset.selected = ""; loadSlots(); });
  loadSlots();

  form.addEventListener("submit", event => {
    if (!servicesLoaded || !form.checkValidity() || !currentOptions().some(opt => opt.checked)) {
      event.preventDefault();
      if (!servicesLoaded) {
        catalogError.classList.remove("d-none");
        catalogError.scrollIntoView({ behavior: "smooth", block: "center" });
      } else if (!currentOptions().some(opt => opt.checked)) {
        container.scrollIntoView({ behavior: "smooth", block: "center" });
        alert("Selecciona al menos un servicio.");
      } else form.reportValidity();
      return;
    }
    submit.disabled = true;
    submit.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Registrando reserva...';
  });
})();
