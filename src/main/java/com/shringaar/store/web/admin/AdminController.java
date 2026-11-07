package com.shringaar.store.web.admin;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.shringaar.store.domain.Category;
import com.shringaar.store.domain.CustomerOrder;
import com.shringaar.store.domain.OrderStatus;
import com.shringaar.store.domain.Product;
import com.shringaar.store.repository.AppointmentRepository;
import com.shringaar.store.repository.CategoryRepository;
import com.shringaar.store.repository.EnquiryRepository;
import com.shringaar.store.repository.OrderRepository;
import com.shringaar.store.repository.ProductRepository;
import com.shringaar.store.service.AppointmentNotifier;
import com.shringaar.store.service.CatalogService;
import com.shringaar.store.service.ImageStorageService;
import com.shringaar.store.service.Phones;
import com.shringaar.store.service.ReminderQueue;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final OrderRepository orders;
    private final EnquiryRepository enquiries;
    private final AppointmentRepository appointments;
    private final CatalogService catalog;
    private final ImageStorageService images;
    private final AppointmentNotifier notifier;
    private final ReminderQueue reminders;

    public AdminController(ProductRepository products, CategoryRepository categories, OrderRepository orders,
                           EnquiryRepository enquiries, AppointmentRepository appointments,
                           CatalogService catalog, ImageStorageService images, AppointmentNotifier notifier,
                           ReminderQueue reminders) {
        this.products = products;
        this.categories = categories;
        this.orders = orders;
        this.enquiries = enquiries;
        this.appointments = appointments;
        this.catalog = catalog;
        this.images = images;
        this.notifier = notifier;
        this.reminders = reminders;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("productCount", products.count());
        model.addAttribute("newOrders", orders.countByStatus(OrderStatus.NEW));
        model.addAttribute("enquiryCount", enquiries.count());
        model.addAttribute("appointmentCount", appointments.count());
        model.addAttribute("remindersPending", notifier.isAutomatic() ? 0 : reminders.pendingCount(Instant.now()));
        model.addAttribute("recentOrders", orders.findAllByOrderByCreatedAtDesc().stream().limit(5).toList());
        model.addAttribute("recentEnquiries", enquiries.findTop200ByOrderByCreatedAtDesc().stream().limit(8).toList());
        return "admin/dashboard";
    }

    // ---- Products ----

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("products", products.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        model.addAttribute("form", new ProductForm());
        model.addAttribute("categories", catalog.categories());
        return "admin/product-form";
    }

    @GetMapping("/products/{id}")
    public String editProduct(@PathVariable Long id, Model model) {
        Product p = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("form", ProductForm.from(p));
        model.addAttribute("categories", catalog.categories());
        return "admin/product-form";
    }

    @PostMapping("/products")
    @Transactional
    public String saveProduct(@Valid @ModelAttribute("form") ProductForm form, BindingResult result,
                              Model model, RedirectAttributes redirect) {
        String uploadedUrl = null;
        if (form.getImage() != null && !form.getImage().isEmpty()) {
            try {
                uploadedUrl = images.store(form.getImage());
            } catch (IllegalArgumentException | IOException e) {
                result.rejectValue("image", "upload", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", catalog.categories());
            return "admin/product-form";
        }

        Product p = form.getId() == null ? new Product()
                : products.findById(form.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Category category = categories.findById(form.getCategoryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));

        p.setName(form.getName().trim());
        p.setSlug(catalog.uniqueSlug(form.getName(), p.getId()));
        p.setCategory(category);
        p.setDetail(form.getDetail());
        p.setDescription(form.getDescription());
        p.setPrice(form.getPrice());
        p.setImageUrl(uploadedUrl != null ? uploadedUrl : form.getImageUrl());
        p.setWashColor(form.getWashColor());
        p.setSizes(form.getSizes());
        p.setMadeToOrder(form.isMadeToOrder());
        p.setLeadTime(form.getLeadTime());
        p.setFeatured(form.isFeatured());
        p.setActive(form.isActive());
        if (form.getSku() != null && !form.getSku().isBlank()) p.setSku(form.getSku().trim());

        Product saved = products.save(p);
        if (saved.getSku() == null) saved.setSku("SS-%04d".formatted(saved.getId()));
        redirect.addFlashAttribute("message", "Saved " + saved.getName());
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/toggle")
    @Transactional
    public String toggleProduct(@PathVariable Long id) {
        products.findById(id).ifPresent(p -> p.setActive(!p.isActive()));
        return "redirect:/admin/products";
    }

    // ---- Categories ----

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", catalog.categories());
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String addCategory(@RequestParam String name, @RequestParam(defaultValue = "0") int sortOrder,
                              RedirectAttributes redirect) {
        String slug = name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (name.isBlank() || slug.isEmpty() || categories.findBySlug(slug).isPresent()) {
            redirect.addFlashAttribute("error", "That category name is empty or already exists.");
        } else {
            categories.save(new Category(name.trim(), slug, sortOrder));
            redirect.addFlashAttribute("message", "Added " + name.trim());
        }
        return "redirect:/admin/categories";
    }

    // ---- Orders ----

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", orders.findAllByOrderByCreatedAtDesc());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable Long id, Model model) {
        CustomerOrder order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("order", order);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("customerWhatsApp", "https://wa.me/" + Phones.toWhatsAppNumber(order.getPhone()));
        return "admin/order";
    }

    @PostMapping("/orders/{id}/status")
    @Transactional
    public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes redirect) {
        CustomerOrder order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        order.setStatus(status);
        redirect.addFlashAttribute("message", "Order " + order.getReference() + " is now " + status.getLabel());
        return "redirect:/admin/orders/" + id;
    }

    // ---- Enquiries & appointments ----

    @GetMapping("/enquiries")
    public String enquiries(Model model) {
        model.addAttribute("enquiries", enquiries.findTop200ByOrderByCreatedAtDesc());
        return "admin/enquiries";
    }

    @GetMapping("/appointments")
    public String appointments(Model model) {
        var list = appointments.findAllByOrderByDateDescTimeDesc();
        Map<Long, String> reminderLinks = new HashMap<>();
        Map<Long, String> chatLinks = new HashMap<>();
        for (var a : list) {
            String number = Phones.toWhatsAppNumber(a.getPhone());
            chatLinks.put(a.getId(), "https://wa.me/" + number);
            reminderLinks.put(a.getId(), "https://wa.me/" + number + "?text="
                    + URLEncoder.encode(notifier.manualReminder(a), StandardCharsets.UTF_8).replace("+", "%20"));
        }
        model.addAttribute("appointments", list);
        model.addAttribute("reminderLinks", reminderLinks);
        model.addAttribute("chatLinks", chatLinks);
        model.addAttribute("autoReminders", notifier.isAutomatic());
        var due = reminders.due(Instant.now());
        Map<String, String> dueLinks = new HashMap<>();
        for (var r : due) dueLinks.put(r.appointment().getId() + "-" + r.kind(), reminders.whatsappLink(r.appointment(), r.kind()));
        model.addAttribute("due", due);
        model.addAttribute("dueLinks", dueLinks);
        return "admin/appointments";
    }

    /** Marks the reminder as sent and opens WhatsApp with it typed out (the form opens in a new tab). */
    @PostMapping("/appointments/{id}/remind")
    public String remind(@PathVariable Long id, @RequestParam ReminderQueue.Kind kind) {
        if (appointments.findById(id).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return "redirect:" + reminders.markSentAndLink(id, kind, Instant.now());
    }

    @PostMapping("/appointments/{id}/remind/undo")
    public String undoRemind(@PathVariable Long id, @RequestParam ReminderQueue.Kind kind) {
        if (appointments.findById(id).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        reminders.undo(id, kind);
        return "redirect:/admin/appointments";
    }

    @PostMapping("/appointments/{id}/confirm")
    @Transactional
    public String confirmAppointment(@PathVariable Long id) {
        appointments.findById(id).ifPresent(a -> a.setConfirmed(!a.isConfirmed()));
        return "redirect:/admin/appointments";
    }


}
