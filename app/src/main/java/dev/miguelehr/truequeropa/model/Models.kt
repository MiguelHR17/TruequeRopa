package dev.miguelehr.truequeropa.model

import com.google.firebase.Timestamp

// ===== ENUMS BÁSICOS =====
enum class Size { XS, S, M, L, XL }
enum class Condition { NUEVO, USADO }
enum class Category { CAMISA, PANTALON, VESTIDO, CHAQUETA, ZAPATOS, ACCESORIO }
enum class ProposalStatus { PENDIENTE, ACEPTADA, RECHAZADA }

// ===== USUARIO MOCK (PARA FakeRepository) =====
data class User(
    val id: String,
    val nombre: String,
    val correo: String,
    val roles: Set<String> = setOf("USER"),
    val tallasPreferidas: Set<Size> = setOf(Size.M),
    val photoUrl: String? = null // opcional: foto de perfil mock
)

// ===== PERFIL REAL EN FIRESTORE (users/{uid}) =====
data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null,
    val active: Boolean = true // ✅ NUEVO: indica si la cuenta está activa
)

// ===== PRODUCTO MOCK (para FakeRepository / ofertas locales) =====
data class Product(
    val id: String,
    val ownerId: String,
    val titulo: String,
    val descripcion: String,
    val talla: Size,
    val estado: Condition,
    val categoria: Category,
    val imageUrl: String
)

// ===== PROPUESTAS DE TRUEQUE (mock) =====
/** AHORA soporta varias prendas ofrecidas (máx 5 a nivel de UI) */
data class TradeProposal(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val offeredProductIds: List<String>,   // varias prendas ofrecidas
    val requestedProductId: String,
    val status: ProposalStatus = ProposalStatus.PENDIENTE
)

// ===== HISTORIAL DE TRUEQUES (mock) =====
data class TradeHistoryItem(
    val id: String,
    val prendaOfrecidaId: String,
    val prendaRecibidaId: String,
    val fecha: String
)

// ===== MÉTRICAS / REPORTES (mock) =====
data class ReportMetrics(
    val usuariosActivos: Int,
    val truequesRealizados: Int,
    val categoriasTop: List<Pair<Category, Int>>,
    val usuariosTop: List<Pair<String, Int>>
)