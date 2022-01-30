package WorkerPackage;

import java.util.List;

public class ProductReview {
    private String title;
    private List<Review> reviews;

    public ProductReview(String title, List<Review> reviews) {
        this.title = title;
        this.reviews = reviews;
    }

    public String getTitle() {
        return title;
    }

    public List<Review> getReviews() {
        return reviews;
    }
}
