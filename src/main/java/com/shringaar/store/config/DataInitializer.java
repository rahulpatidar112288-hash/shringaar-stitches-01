package com.shringaar.store.config;

import java.math.BigDecimal;

import com.shringaar.store.domain.Category;
import com.shringaar.store.domain.Product;
import com.shringaar.store.repository.CategoryRepository;
import com.shringaar.store.repository.ProductRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a starter catalogue into an empty database so the site has something
 * to show. Prices are SAMPLE values: edit them in /admin/products.
 * Turn off with store.seed-sample-data=false.
 */
@Component
@ConditionalOnProperty(name = "store.seed-sample-data", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryRepository categories;
    private final ProductRepository products;

    public DataInitializer(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categories.count() > 0) return;

        Category bridal = categories.save(new Category("Bridal", "bridal", 1));
        Category festive = categories.save(new Category("Festive", "festive", 2));
        Category blouses = categories.save(new Category("Blouses", "blouses", 3));
        Category suits = categories.save(new Category("Suits", "suits", 4));

        String women = "XS,S,M,L,XL,Custom";

        save("SS-0001", "Rani pink lehenga", bridal, "Raw silk · zardozi", "#F3E1E4", "85000", women, true,
                "A rani pink raw-silk lehenga with hand zardozi on the kalis and border, paired with a net dupatta. Cut to your measurements over three fittings.");
        save("SS-0002", "Gota patti anarkali", festive, "Georgette · gota work", "#F3E6C8", "18500", women, true,
                "A floor-length georgette anarkali with gota patti along the yoke and hem. Light enough for a day-long mehendi.");
        save("SS-0003", "Mirror-work blouse", blouses, "Cotton silk · hand mirror", "#F1E9DE", "6500", women, true,
                "A cotton-silk saree blouse with hand-set mirrors on the sleeves and back. Choose the neckline at your fitting.");
        save("SS-0004", "Ivory chikankari kurta set", suits, "Georgette · chikankari", "#EDE6DA", "14500", women, true,
                "A hand-embroidered chikankari kurta in ivory georgette, with straight pants and an organza dupatta.");
        save("SS-0005", "Maroon velvet lehenga", bridal, "Velvet · dabka and nakshi", "#EBD3D8", "120000", women, false,
                "Deep maroon velvet with dabka and nakshi embroidery, for winter weddings and receptions.");
        save("SS-0006", "Pastel organza sharara", festive, "Organza · pearl work", "#E9EEF0", "24000", women, false,
                "A pastel organza sharara set finished with hand-sewn pearls. Pairs with a short kurta and dupatta.");
        save("SS-0007", "Zari brocade blouse", blouses, "Banarasi brocade", "#F3E6C8", "7800", women, false,
                "A Banarasi brocade blouse with a sweetheart neckline and tassel tie-back.");
        save("SS-0008", "Phulkari salwar suit", suits, "Cotton silk · phulkari", "#E6E1D8", "11000", women, false,
                "A cotton-silk salwar suit with a hand-worked phulkari dupatta. Made to measure.");

        log.info("Loaded sample catalogue: {} products. Edit prices in /admin/products.", products.count());
    }

    private void save(String sku, String name, Category c, String detail, String wash, String price,
                      String sizes, boolean featured, String description) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setSlug(name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""));
        p.setCategory(c);
        p.setDetail(detail);
        p.setWashColor(wash);
        p.setPrice(new BigDecimal(price));
        p.setSizes(sizes);
        p.setFeatured(featured);
        p.setDescription(description);
        products.save(p);
    }
}
