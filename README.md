## Running the sample code

1. Start a local PostgresSQL server on default port 5432 and a Kafka broker on port 9092. The included `docker-compose.yml` starts everything required for running locally.

    ```shell
    docker-compose up -d

    # creates the tables needed for Akka Persistence
    # as well as the offset store table for Akka Projection
    docker exec -i akka-replicated-event-sourcing-grpc_postgres-db-a_1 psql -U shopping-cart -t < ddl-scripts/create_tables.sql
   
    docker exec -i akka-replicated-event-sourcing-grpc_postgres-db-b_1 psql -U shopping-cart -t < ddl-scripts/create_tables.sql
    
    # creates the user defined projection table.
    docker exec -i akka-replicated-event-sourcing-grpc_postgres-db-a_1 psql -U shopping-cart -t < ddl-scripts/create_user_tables.sql
   
    docker exec -i akka-replicated-event-sourcing-grpc_postgres-db-b_1 psql -U shopping-cart -t < ddl-scripts/create_user_tables.sql
    ```

2. Start a first node:

    ```
    sbt -Dconfig.resource=local1.conf run
    ```

3. Try it with [grpcurl](https://github.com/fullstorydev/grpcurl):

    ```shell
    # add item to cart
    grpcurl -d '{"cartId":"cart1", "itemId":"hoodie", "quantity":5}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.AddItem
   
    # check the quantity of the cart
    grpcurl -d '{"cartId":"cart1"}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.GetCart
   
    # get popularity
    grpcurl -d '{"itemId":"hoodie"}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.GetItemPopularity
    ```

4. Start a second node:

    ```
    sbt -Dconfig.resource=local2.conf run
    ```

5. check the quantity of replicated cart1

    ```shell  
    grpcurl -d '{"cartId":"cart1"}' -plaintext 127.0.0.1:8102 shoppingcart.ShoppingCartService.GetCart   
    ```

    Note that the first request will return "not found", but once activated it will include the replicated event.

6. update from node2

    ```shell  
    grpcurl -d '{"cartId":"cart2", "itemId":"t-shirt", "quantity":2}' -plaintext 127.0.0.1:8102 shoppingcart.ShoppingCartService.AddItem   
   
    # check quantity from node1
    grpcurl -d '{"cartId":"cart2"}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.GetCart
    ```


