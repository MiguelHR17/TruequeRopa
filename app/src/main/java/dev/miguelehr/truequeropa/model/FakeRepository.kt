package dev.miguelehr.truequeropa.model

object FakeRepository {

    // Usuario actual
    val currentUser = User(
        id = "u1",
        nombre = "Miguel Hernández",
        correo = "miguel@correo.com",
        roles = setOf("USER", "ADMIN"),
        tallasPreferidas = setOf(Size.M, Size.L)
    )

    // Lista de usuarios
    val users = mutableListOf(
        currentUser,
        User("u2", "Andrea Paniagua", "andrea@correo.com"),
        User("u3", "Marco Cajusol", "marco@correo.com")
    )

    // Lista de productos
    val products = mutableListOf(
        // 🔽 NUEVA PRENDA DEL USUARIO ACTUAL (A)
        Product(
            id = "p0",
            ownerId = "u1",
            titulo = "Chaqueta gris",
            descripcion = "Casi nueva, usada 2 veces",
            talla = Size.M,
            estado = Condition.USADO,
            categoria = Category.CHAQUETA,
            imageUrl = "https://picsum.photos/seed/self/600/400"
        ),

        // 🔽 Prendas de otros usuarios
        Product("p1", "u2", "Camisa azul", "Algodón 100%", Size.M, Condition.USADO, Category.CAMISA, "https://picsum.photos/seed/p1/600/400"),
        Product("p2", "u3", "Pantalón negro", "Slim fit", Size.L, Condition.NUEVO, Category.PANTALON, "https://picsum.photos/seed/p2/600/400"),
        Product("p3", "u2", "Vestido rojo", "Fiesta", Size.S, Condition.USADO, Category.VESTIDO, "https://picsum.photos/seed/p3/600/400")
    )

    // Propuestas y trueques
    val proposals = mutableListOf<TradeProposal>()
    val trades = mutableListOf<TradeHistoryItem>()

    // Métricas generales
    fun metrics() = ReportMetrics(
        usuariosActivos = 123,
        truequesRealizados = 42,
        categoriasTop = listOf(
            Category.CAMISA to 18,
            Category.PANTALON to 12,
            Category.VESTIDO to 9
        ),
        usuariosTop = listOf(
            "Andrea" to 10,
            "Marco" to 8,
            "Miguel" to 7
        )
    )
}