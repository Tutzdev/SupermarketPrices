package br.com.supermercados.prices.shoppinglist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, UUID> {

    @Query("""
            select new br.com.supermercados.prices.shoppinglist.ShoppingListItemResponse(
                item.id, item.productId, product.name, item.quantity)
            from ShoppingListItem item join Product product on product.id = item.productId
            where item.shoppingListId = :listId
            order by product.name, item.id
            """)
    List<ShoppingListItemResponse> findItems(UUID listId);

    Optional<ShoppingListItem> findByIdAndShoppingListId(UUID id, UUID shoppingListId);

    boolean existsByShoppingListIdAndProductId(UUID shoppingListId, UUID productId);

    long countByShoppingListId(UUID shoppingListId);
}
