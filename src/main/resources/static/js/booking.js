// Booking page: grey out time slots already taken on the chosen day.
(function () {
  var form = document.getElementById('booking-form');
  if (!form) return;
  var taken = (form.dataset.taken || '').split(',').filter(Boolean);

  function normalise(t) { return t.length === 5 ? t : t.slice(0, 5); }

  function update() {
    var day = form.querySelector('input[name="date"]:checked');
    form.querySelectorAll('input[name="time"]').forEach(function (input) {
      var isTaken = day && taken.indexOf(day.value + 'T' + normalise(input.value)) !== -1;
      input.disabled = isTaken;
      if (isTaken && input.checked) input.checked = false;
    });
  }

  form.addEventListener('change', update);
  update();
})();
