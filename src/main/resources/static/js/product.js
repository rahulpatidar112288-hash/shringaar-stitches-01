// Product page: quantity stepper, and keep the WhatsApp enquiry links in sync
// with the chosen size and quantity so the pre-filled message includes them.
(function () {
  var form = document.getElementById('buy-form');
  if (!form) return;
  var qty = form.querySelector('#qty');
  var links = [document.getElementById('wa-enquire'), document.querySelector('.wa-fab')].filter(Boolean);
  var base = links.length ? links[0].getAttribute('href').split('?')[0] : null;

  form.querySelectorAll('[data-step]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var v = parseInt(qty.value || '1', 10) + parseInt(btn.dataset.step, 10);
      qty.value = Math.min(10, Math.max(1, v));
      sync();
    });
  });

  function sync() {
    if (!base) return;
    var params = new URLSearchParams();
    var size = form.querySelector('input[name="size"]:checked');
    if (size) params.set('size', size.value);
    if (parseInt(qty.value, 10) > 1) params.set('qty', qty.value);
    var href = base + (params.toString() ? '?' + params.toString() : '');
    links.forEach(function (a) { a.setAttribute('href', href); });
  }

  form.addEventListener('change', sync);
  form.addEventListener('input', sync);
})();
