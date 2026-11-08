package com.azuredevid.cassy.data.model

data class CatalogProduct(
    val id: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val description: String,
    val basePriceIdr: Long,
    val imageUrl: String,
    val prepStation: String,
    val isAvailable: Boolean = true,
    val stockQuantity: Int = 50
)

object SeedCatalog {
    val sampleProducts = listOf(
        CatalogProduct(
            id = "d0000001-0000-4000-8000-000000000001",
            name = "Classic Iced Latte",
            categoryId = "signature",
            categoryName = "Signature",
            description = "Double espresso shot with fresh dairy milk and artisanal sugar",
            basePriceIdr = 24000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/classic-iced-latte-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000007-0000-4000-8000-000000000007",
            name = "Es Kopi Susu Aren Senopati",
            categoryId = "signature",
            categoryName = "Signature",
            description = "Double ristretto house blend dengan nektar gula aren organik Jawa Barat dan susu segar",
            basePriceIdr = 26000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/kopi-susu-gula-aren-senopati-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000008-0000-4000-8000-000000000008",
            name = "Salted Caramel Cloud Macchiato",
            categoryId = "signature",
            categoryName = "Signature",
            description = "Espresso single origin di atas susu vanilla Madagaskar dengan lapisan caramel cold foam",
            basePriceIdr = 34000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/salted-caramel-cloud-macchiato-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000009-0000-4000-8000-000000000009",
            name = "Pistachio Crema Cold Latte",
            categoryId = "signature",
            categoryName = "Signature",
            description = "Paduan pasta pistachio panggang Sisilia, double ristretto, dan taburan kacang pistachio",
            basePriceIdr = 38000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/pistachio-crema-cold-latte-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000010-0000-4000-8000-000000000010",
            name = "Yuzu Espresso Tonica",
            categoryId = "signature",
            categoryName = "Signature",
            description = "Segarnya citrus yuzu Jepang berpadu dengan botanical tonic water dan double espresso",
            basePriceIdr = 34000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/yuzu-espresso-tonica-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000002-0000-4000-8000-000000000002",
            name = "Caffe Latte",
            categoryId = "espresso",
            categoryName = "Espresso",
            description = "Espresso with steamed fresh milk and microfoam",
            basePriceIdr = 26000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/caffe-latte-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000003-0000-4000-8000-000000000003",
            name = "Americano (Double Shot)",
            categoryId = "espresso",
            categoryName = "Espresso",
            description = "Double ristretto poured over filtered water",
            basePriceIdr = 20000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/americano-double-shot-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000011-0000-4000-8000-000000000011",
            name = "Velvet Cappuccino",
            categoryId = "espresso",
            categoryName = "Espresso",
            description = "Double espresso dengan microfoam tebal bertekstur beludru dan bubuk kakao Valrhona",
            basePriceIdr = 28000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/velvet-cappuccino-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000012-0000-4000-8000-000000000012",
            name = "Flat White Double Ristretto",
            categoryId = "espresso",
            categoryName = "Espresso",
            description = "Ekstraksi 20g double ristretto pekat dengan lapisan tipis steamed milk 62°C",
            basePriceIdr = 30000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/flat-white-double-ristretto-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000013-0000-4000-8000-000000000013",
            name = "Piccolo Latte (4 oz)",
            categoryId = "espresso",
            categoryName = "Espresso",
            description = "Single ristretto shot dalam gelas 120ml dengan rasio susu hangat yang intens dan manis",
            basePriceIdr = 24000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/piccolo-latte-4oz-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000004-0000-4000-8000-000000000004",
            name = "V60 Single Origin Gayo",
            categoryId = "manual_brew",
            categoryName = "Manual Brew",
            description = "Filter brewed single origin with floral citrus notes",
            basePriceIdr = 32000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/v60-single-origin-gayo-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000014-0000-4000-8000-000000000014",
            name = "Japanese Iced Filter Bali Kintamani",
            categoryId = "manual_brew",
            categoryName = "Manual Brew",
            description = "Seduhan pour-over langsung di atas es dengan karakter jeruk manis dan gula merah",
            basePriceIdr = 34000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/japanese-iced-filter-kintamani-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000015-0000-4000-8000-000000000015",
            name = "Kalita Wave Flores Bajawa",
            categoryId = "manual_brew",
            categoryName = "Manual Brew",
            description = "Seduhan flat-bottom dengan aroma cokelat hitam dan rempah manis",
            basePriceIdr = 35000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/kalita-wave-flores-bajawa-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000016-0000-4000-8000-000000000016",
            name = "Nitro Cold Brew Cascade",
            categoryId = "manual_brew",
            categoryName = "Manual Brew",
            description = "Cold brew 18 jam diinfusi gas nitrogen murni dari tap dengan efek kaskade lembut",
            basePriceIdr = 36000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/nitro-cold-brew-cascade-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000005-0000-4000-8000-000000000005",
            name = "Butter Croissant Artisanal",
            categoryId = "pastry",
            categoryName = "Pastry & Bakery",
            description = "Freshly baked flaky French butter croissant",
            basePriceIdr = 24000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/butter-croissant-artisanal-1x1.webp",
            prepStation = "kitchen"
        ),
        CatalogProduct(
            id = "d0000017-0000-4000-8000-000000000017",
            name = "Pain au Chocolat Valrhona",
            categoryId = "pastry",
            categoryName = "Pastry & Bakery",
            description = "Pastri viennoiserie renyah dengan cokelat hitam Valrhona 66%",
            basePriceIdr = 28000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/pain-au-chocolat-valrhona-1x1.webp",
            prepStation = "kitchen"
        ),
        CatalogProduct(
            id = "d0000018-0000-4000-8000-000000000018",
            name = "Caramelized Almond Kouign-Amann",
            categoryId = "pastry",
            categoryName = "Pastry & Bakery",
            description = "Pastri klasik Brittany dengan kerak gula karamel asin dan irisan almond panggang",
            basePriceIdr = 32000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/almond-kouign-amann-1x1.webp",
            prepStation = "kitchen"
        ),
        CatalogProduct(
            id = "d0000019-0000-4000-8000-000000000019",
            name = "Basque Burnt Cheesecake Slice",
            categoryId = "pastry",
            categoryName = "Pastry & Bakery",
            description = "Cheesecake suhu tinggi bergaya San Sebastian dengan bagian tengah lumer",
            basePriceIdr = 42000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/basque-burnt-cheesecake-1x1.webp",
            prepStation = "kitchen"
        ),
        CatalogProduct(
            id = "d0000020-0000-4000-8000-000000000020",
            name = "Uji Ceremonial Matcha Latte",
            categoryId = "non_coffee",
            categoryName = "Non-Coffee",
            description = "Matcha panen pertama Uji Kyoto dikocok chasen bambu di atas susu segar",
            basePriceIdr = 36000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/uji-ceremonial-matcha-latte-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000021-0000-4000-8000-000000000021",
            name = "Hojicha Roasted Oat Latte",
            categoryId = "non_coffee",
            categoryName = "Non-Coffee",
            description = "Teh hijau panggang khas Jepang dengan aroma smoky karamel dan oat milk",
            basePriceIdr = 35000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/hojicha-roasted-oat-latte-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000022-0000-4000-8000-000000000022",
            name = "72% Single Origin Choco Karo",
            categoryId = "non_coffee",
            categoryName = "Non-Coffee",
            description = "Cokelat hitam single-origin Tanah Karo Sumatera dengan tekstur kental buah beri",
            basePriceIdr = 34000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/single-origin-dark-choco-karo-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000023-0000-4000-8000-000000000023",
            name = "Hibiscus Berry Cold Brew Tea",
            categoryId = "non_coffee",
            categoryName = "Non-Coffee",
            description = "Seduhan dingin kelopak rosella merah, buah beri hutan, dan daun mint segar",
            basePriceIdr = 28000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/hibiscus-berry-artisan-tea-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000024-0000-4000-8000-000000000024",
            name = "Truffle Egg & Smoked Beef Brioche",
            categoryId = "meals",
            categoryName = "Heavy Meals",
            description = "Brioche mentega dengan telur soft-scramble, smoked beef brisket, dan truffle aioli",
            basePriceIdr = 58000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/truffle-scrambled-egg-brioche-1x1.webp",
            prepStation = "kitchen"
        ),
        CatalogProduct(
            id = "d0000025-0000-4000-8000-000000000025",
            name = "Crispy Chicken Sambal Matah Bowl",
            categoryId = "meals",
            categoryName = "Heavy Meals",
            description = "Nasi pandan wangi, ayam goreng renyah, sambal matah Bali, dan telur onsen",
            basePriceIdr = 52000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/crispy-chicken-sambal-matah-bowl-1x1.webp",
            prepStation = "kitchen"
        ),
        CatalogProduct(
            id = "d0000026-0000-4000-8000-000000000026",
            name = "Wagyu Beef Lasagna Al Forno",
            categoryId = "meals",
            categoryName = "Heavy Meals",
            description = "Pasta segar dengan ragu sapi Wagyu masak lambat 6 jam, bechamel, dan Parmigiano",
            basePriceIdr = 68000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/wagyu-beef-lasagna-al-forno-1x1.webp",
            prepStation = "kitchen"
        ),
        CatalogProduct(
            id = "d0000027-0000-4000-8000-000000000027",
            name = "Truffle Parmesan Pommes Frites",
            categoryId = "meals",
            categoryName = "Heavy Meals",
            description = "Kentang goreng renyah dengan minyak truffle putih, Grana Padano, dan rosemary",
            basePriceIdr = 42000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/truffle-parmesan-pommes-frites-1x1.webp",
            prepStation = "kitchen"
        ),
        CatalogProduct(
            id = "d0000028-0000-4000-8000-000000000028",
            name = "Espresso Crema House Blend (Beans)",
            categoryId = "beans",
            categoryName = "Beans & Bottled",
            description = "Biji kopi sangrai medium-dark (70% Arabika Aceh Gayo, 30% Robusta Temanggung)",
            basePriceIdr = 95000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/roastery-house-blend-beans-1x1.webp",
            prepStation = "none"
        ),
        CatalogProduct(
            id = "d0000029-0000-4000-8000-000000000029",
            name = "Aceh Gayo Natural Wine Process",
            categoryId = "beans",
            categoryName = "Beans & Bottled",
            description = "Biji kopi single origin Takengon fermentasi natural aroma buah anggur dan kakao",
            basePriceIdr = 125000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/aceh-gayo-wine-process-beans-1x1.webp",
            prepStation = "none"
        ),
        CatalogProduct(
            id = "d0000006-0000-4000-8000-000000000006",
            name = "Express Cold Brew",
            categoryId = "beans",
            categoryName = "Beans & Bottled",
            description = "Steeped for 18 hours, rich and smooth finish",
            basePriceIdr = 22000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/express-cold-brew-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000030-0000-4000-8000-000000000030",
            name = "Es Kopi Susu Komuter MRT",
            categoryId = "signature",
            categoryName = "Signature",
            description = "Kopi susu gula aren khas komuter disajikan cepat dalam cup segel anti-tumpah",
            basePriceIdr = 20000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/es-kopi-susu-komuter-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000031-0000-4000-8000-000000000031",
            name = "Shaken Brown Sugar Americano",
            categoryId = "espresso",
            categoryName = "Espresso",
            description = "Double espresso dikocok dengan es kristal dan molase gula merah buih lembut",
            basePriceIdr = 22000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/iced-shaken-brown-sugar-americano-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000032-0000-4000-8000-000000000032",
            name = "Express Iced Matcha Boost",
            categoryId = "non_coffee",
            categoryName = "Non-Coffee",
            description = "Matcha Kyoto dingin dengan susu segar untuk energi pagi bebas kafein tinggi",
            basePriceIdr = 26000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/express-iced-matcha-boost-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000033-0000-4000-8000-000000000033",
            name = "Iced Belgian Choco Malt",
            categoryId = "non_coffee",
            categoryName = "Non-Coffee",
            description = "Es cokelat Belgia pekat dengan taburan malt renyah di atas milk foam",
            basePriceIdr = 25000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/iced-belgian-choco-malt-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000034-0000-4000-8000-000000000034",
            name = "Botol Kopi Susu Aren (500ml)",
            categoryId = "beans",
            categoryName = "Beans & Bottled",
            description = "Kemasan botol 500ml (3-4 porsi) siap simpan di kulkas rumah atau kantor",
            basePriceIdr = 55000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/botol-kopi-susu-aren-500ml-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000035-0000-4000-8000-000000000035",
            name = "Botol Cold Brew Black (500ml)",
            categoryId = "beans",
            categoryName = "Beans & Bottled",
            description = "Konsentrat cold brew single origin bebas gula dalam botol segel 500ml",
            basePriceIdr = 52000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/botol-cold-brew-black-500ml-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000036-0000-4000-8000-000000000036",
            name = "Smoked Beef & Cheddar Croissant",
            categoryId = "pastry",
            categoryName = "Pastry & Bakery",
            description = "Butter croissant hangat berisi daging sapi asap dan keju red cheddar leleh",
            basePriceIdr = 34000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/smoked-beef-cheddar-croissant-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000037-0000-4000-8000-000000000037",
            name = "Cinnamon Sugar Brioche Cruffin",
            categoryId = "pastry",
            categoryName = "Pastry & Bakery",
            description = "Kombinasi croissant dan muffin gula kayu manis dengan isian krim vanila",
            basePriceIdr = 26000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/cinnamon-sugar-cruffin-1x1.webp",
            prepStation = "bar"
        ),
        CatalogProduct(
            id = "d0000038-0000-4000-8000-000000000038",
            name = "Banana Walnut Espresso Loaf",
            categoryId = "pastry",
            categoryName = "Pastry & Bakery",
            description = "Bolu pisang raja panggang lembap dengan kenari dan sentuhan espresso",
            basePriceIdr = 24000L,
            imageUrl = "https://cdn.abbdullaziz.online/seeds/products/banana-walnut-coffee-cake-1x1.webp",
            prepStation = "bar"
        )
    )
}

