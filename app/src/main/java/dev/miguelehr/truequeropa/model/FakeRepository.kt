package dev.miguelehr.truequeropa.model

object FakeRepository {
    val currentUser = User("u1","Miguel Hernández","miguel@correo.com", roles=setOf("USER","ADMIN"), tallasPreferidas=setOf(Size.M, Size.L))

    val users = mutableListOf(
        currentUser,
        User("u2","Andrea Paniagua","andrea@correo.com"),
        User("u3","Marco Cajusol","marco@correo.com")
    )

    val products = mutableListOf(
        Product("p1","u2","Camisa azul","Algodón 100%", Size.M, Condition.USADO, Category.CAMISA,"https://picsum.photos/seed/p1/600/400"),
        Product("p2","u3","Pantalón negro","Slim fit", Size.L, Condition.NUEVO, Category.PANTALON,"https://picsum.photos/seed/p2/600/400"),
        Product("p3","u2","Vestido rojo","Fiesta", Size.S, Condition.USADO, Category.VESTIDO,"https://picsum.photos/seed/p3/600/400"),
        // Asegúrate de tener prendas del usuario actual (u1) para poder OFRECER
        Product("m1","u1","Polo azul","Algodón", Size.M, Condition.USADO, Category.CAMISA,"https://picsum.photos/seed/m1/600/400"),
        Product("m2","u1","Jean gris","Tiro alto", Size.L, Condition.NUEVO, Category.PANTALON,"https://picsum.photos/seed/m2/600/400"),
        Product("m3","u1","Casaca negra","Cierre metálico", Size.L, Condition.USADO, Category.CHAQUETA,"https://picsum.photos/seed/m3/600/400")
    )

    val proposals = mutableListOf<TradeProposal>()
    val trades = mutableListOf<TradeHistoryItem>()

    fun sendProposal(fromUserId: String, toUserId: String, offeredProductIds: List<String>, requestedProductId: String) {
        proposals += TradeProposal(
            id = "pr${proposals.size + 1}",
            fromUserId = fromUserId,
            toUserId = toUserId,
            offeredProductIds = offeredProductIds,
            requestedProductId = requestedProductId
        )
    }

    fun acceptProposal(p: TradeProposal) {
        trades += TradeHistoryItem(
            id = "t${trades.size + 1}",
            prendaOfrecidaId = p.offeredProductIds.firstOrNull() ?: "", // para historial simple (mock)
            prendaRecibidaId = p.requestedProductId,
            fecha = "2025-11-07"
        )
        proposals.remove(p)
    }

    fun rejectProposal(p: TradeProposal) {
        proposals.remove(p)
    }

    fun metrics() = ReportMetrics(
        usuariosActivos = 123,
        truequesRealizados = 42,
        categoriasTop = listOf(Category.CAMISA to 18, Category.PANTALON to 12, Category.VESTIDO to 9),
        usuariosTop = listOf("Andrea" to 10, "Marco" to 8, "Miguel" to 7)
    )

     fun generateImageUrl(category: String, index: Int): String {
        // Convertimos la categoría del Enum a un String legible para la URL (ej: CAMISA -> "shirt")
        val categoryForUrl = when (category) {
            "CAMISA" -> "shirt"
            "PANTALON" -> "pants"
            "VESTIDO" -> "dress"
            "CHAQUETA" -> "jacket"
            "ZAPATOS" -> "shoe"
            else -> "clothes"
        }
        // Construimos y devolvemos la URL completa
        return "https://placeholdxr.com/api/image/$categoryForUrl?index=$index"
    }

}