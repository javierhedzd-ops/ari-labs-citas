package mx.arilabs.citas.datos

/** Repertorio para empezar: piezas conocidas en misas, bodas, XV y rezos. Él lo cambia a su gusto. */
object Repertorio {

    fun inicial(): List<Cancion> {
        var id = 1L
        fun c(titulo: String, momento: Momento, tono: String = "") = Cancion(id++, titulo, tono, momento)
        return listOf(
            c("Marcha nupcial (Mendelssohn)", Momento.ENTRADA, "Do"),
            c("Canon en Re (Pachelbel)", Momento.ENTRADA, "Re"),
            c("Marcha nupcial (Wagner)", Momento.ENTRADA, "Si♭"),
            c("Pescador de hombres", Momento.MISA, "Re"),
            c("Vienen con alegría", Momento.MISA, "Sol"),
            c("Aleluya", Momento.MISA),
            c("Santo", Momento.MISA),
            c("Cordero de Dios", Momento.MISA),
            c("Ave María (Schubert)", Momento.COMUNION, "Si♭"),
            c("Ave María (Bach-Gounod)", Momento.COMUNION, "Do"),
            c("Alma misionera", Momento.COMUNION, "Re"),
            c("La Guadalupana", Momento.SALIDA, "Re"),
            c("Danubio azul", Momento.VALS, "Re"),
            c("Sobre las olas", Momento.VALS, "Sol"),
            c("Tiempo de vals", Momento.VALS),
            c("Las mañanitas", Momento.FIESTA, "Sol"),
            c("Mi viejo", Momento.FIESTA),
            c("Dios te salve, María", Momento.REZO),
            c("Más allá del sol", Momento.REZO),
            c("Oh María, madre mía", Momento.REZO),
        )
    }

    /** Lo que se le sugiere poner en cada tipo de evento, en este orden. */
    fun momentosDe(tipo: Tipo): List<Momento> = when (tipo) {
        Tipo.MISA -> listOf(Momento.ENTRADA, Momento.MISA, Momento.COMUNION, Momento.SALIDA)
        Tipo.BODA, Tipo.XV -> listOf(Momento.ENTRADA, Momento.VALS, Momento.FIESTA)
        Tipo.FIESTA -> listOf(Momento.FIESTA)
        Tipo.REZO, Tipo.NOVENA, Tipo.VELORIO -> listOf(Momento.REZO)
        Tipo.OTRO -> Momento.entries
    }

    /** Lo que se suele llevar a un evento. */
    val equipoInicial = listOf("Teclado", "Bocina", "Cable de la bocina", "Extensión", "Atril", "Banco", "Pedal", "Cargador")
}
