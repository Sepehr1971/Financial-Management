package com.example.data.repository

/**
 * Robust Persian text and category matcher for folders and expenses.
 * Solves duplicate folder tabs, semantic isolation between categories (e.g. clothing vs fruits vs snacks),
 * and isolates annual insurance from vehicle/motorcycle maintenance.
 */
object FolderMatcher {

    /**
     * Standardizes Persian characters, trims, removes diacritics and replaces ZWNJ with space.
     */
    fun normalize(text: String): String {
        return text
            .replace('\u064A', '\u06CC') // Arabic Yeh to Persian Yeh (ي -> ی)
            .replace('\u0643', '\u06A9') // Arabic Kaf to Persian Kaf (ك -> ک)
            .replace('\u0629', '\u0647') // Teh Marbuta to Heh (ة -> ه)
            .replace('\u200C', ' ')       // Zero-Width Non-Joiner (نیم‌فاصله) to space
            .replace("  +", " ")
            .trim()
    }

    /**
     * Strips all whitespace, ZWNJs, and punctuation for strict semantic key matching.
     */
    fun toKey(text: String): String {
        val norm = normalize(text)
        return norm
            .replace(" ", "")
            .replace("‌", "")
            .replace("،", "")
            .replace("-", "")
            .replace("_", "")
            .replace("/", "")
            .lowercase()
    }

    /**
     * Strips leading Persian prefixes like "پول", "خرج", "هزینه", "هزینه‌های", "سهم", "بابت", "برای"
     * so that "پول فاطمه" and "فاطمه" or "پول علی" and "علی" resolve to the exact same canonical category.
     */
    fun cleanFolderName(text: String): String {
        var s = normalize(text)
        val prefixes = listOf(
            "هزینه‌های شخصی", "هزینه های شخصی",
            "هزینه‌های", "هزینه های", "هزینهٔ", "هزینه", "هزینها", "هزینهها",
            "پولِ", "پول", "خرجِ", "خرج", "سهمِ", "سهم",
            "بابتِ", "بابت", "برایِ", "برای", "واریز به", "واریز"
        )
        for (p in prefixes) {
            if (s.startsWith(p)) {
                val rem = s.substring(p.length).trim()
                if (rem.length >= 2) {
                    s = rem
                    break
                }
            }
        }
        return s
    }

    const val UNIFIED_MOTOR_FOLDER_NAME = "موتور"

    /**
     * Maps any folder name or expense title to a canonical group ID.
     * Guarantees:
     * - "موتور", "نگهداری موتور", and "بیمه موتورسیکلت" all map to unified "MOTOR"
     * - "هزینه‌های نگهداری ماشین" and "هزینه نگهداری ماشین" map to "CAR_MAINTENANCE"
     * - "بنزین و حمل و نقل" and "بنزین و حمل‌ونقل" map to "TRANSPORT"
     * - "پوشاک" and "پوشاک و خرید شخصی" map to "CLOTHING"
     * - "خرید میوه" maps to "FRUIT"
     * - "خرید مرغ" maps to "CHICKEN_MEAT"
     * - "خرید تنقلات" maps to "SNACKS"
     * - "سوپرمارکت و خوراکی" maps to "SUPERMARKET"
     * - "پول فاطمه" and "فاطمه" map to "فاطمه"
     * - "پول علی" and "علی" map to "علی"
     */
    fun getCanonicalGroupId(name: String): String {
        val norm = normalize(name)
        val key = toKey(norm)
        val cleaned = cleanFolderName(norm)
        val cleanedKey = toKey(cleaned)

        return when {
            // Motorcycle: Unifies all motorcycle expenses: maintenance, repairs, fuel, parts,
            // AND annual motorcycle commitments (insurance/fees) amortized across 12 months!
            key.contains("موتور") -> "MOTOR"

            // Annual insurance / commitments (ALWAYS separate from maintenance!)
            key.contains("بیمه") || key.contains("عوارض") || key.contains("سالانه") || key.contains("تعهدات") -> "ANNUAL"

            // Car maintenance & vehicle upkeep map to TRANSPORT (بنزین و حمل‌ونقل) - no separate CAR_MAINTENANCE item
            (key.contains("ماشین") || key.contains("خودرو")) && (key.contains("نگهداری") || key.contains("تعمیر") || key.contains("سرویس") || key.contains("روغن")) -> "TRANSPORT"
            key == "هزینهنگهدارىماشین" || key == "هزینه‌هاینگهدارىماشین" || key == "نگهدارىماشین" || key == "تعمیرماشین" -> "TRANSPORT"

            // Gasoline & Transport
            key.contains("بنزین") || key.contains("حملونقل") || key.contains("کرایه") || key == "اسنپ" || key == "تپسی" || key == "تاکسی" || key == "بنزینوحملونقل" -> "TRANSPORT"

            // Clothing & Personal Apparel
            key.contains("پوشاک") || key.contains("لباس") || key.contains("شلوار") || key.contains("پیراهن") || key.contains("کفش") || key == "پوشاکوخریدشخصی" -> "CLOTHING"

            // Specific Food Groups (MUST REMAIN STRICTLY ISOLATED!)
            key.contains("مرغ") || key.contains("گوشت") -> "CHICKEN_MEAT"
            key.contains("میوه") || key.contains("تره‌بار") || key.contains("ترهبار") -> "FRUIT"
            key.contains("تنقلات") || key.contains("آجیل") || key.contains("شیرینی") || key.contains("چیپس") || key.contains("پفک") -> "SNACKS"
            key.contains("نان") || key.contains("نانوایی") -> "BAKERY"

            // General Supermarket & Groceries
            key.contains("سوپرمارکت") || key.contains("سوپر") || key.contains("سوپرمارکتوخوراکی") -> "SUPERMARKET"

            // Rent (Housing Rent - Strictly separate from bills)
            key.contains("اجاره") || key.contains("مسکن") || key.contains("رهن") -> "RENT"

            // Utility Bills (Power, Gas, Water, Telephone, Building Charge - Strictly separate from rent)
            key.contains("قبض") || key.contains("قبوض") || key.contains("برق") || key.contains("گاز") || key.contains("آب") || key.contains("تلفن") || key.contains("شارژساختمان") || key.contains("شارژ") -> "BILLS"

            // Restaurant & Cafe
            key.contains("رستوران") || key.contains("کافه") || key.contains("فستفود") || key.contains("پیتزا") -> "RESTAURANT"

            // Health & Medical (non-annual direct medical, e.g. doctor, pharmacy)
            key.contains("پزشک") || key.contains("دارو") || key.contains("درمان") || key.contains("دندان") || key.contains("کلینیک") || key.contains("بیمارستان") -> "MEDICAL"

            // Internet
            key.contains("اینترنت") || key.contains("وایفای") || key.contains("مودم") || key.contains("بستهاینترنت") -> "INTERNET"

            // Default: strict sanitized & prefix-cleaned key (e.g. "پول فاطمه" and "فاطمه" both yield "فاطمه")
            else -> cleanedKey
        }
    }

    /**
     * Checks if two folder names refer to the same logical folder.
     */
    fun isSameFolder(folder1: String, folder2: String): Boolean {
        if (folder1.equals(folder2, ignoreCase = true)) return true
        val k1 = toKey(folder1)
        val k2 = toKey(folder2)
        if (k1 == k2) return true
        val c1 = toKey(cleanFolderName(folder1))
        val c2 = toKey(cleanFolderName(folder2))
        if (c1 == c2) return true
        val g1 = getCanonicalGroupId(folder1)
        val g2 = getCanonicalGroupId(folder2)
        return g1 == g2
    }

    /**
     * Authoritative matching algorithm for filtering expenses by a selected folder tab.
     */
    fun matches(
        expenseEffectiveFolder: String,
        expenseCustomFolder: String?,
        expenseTitle: String,
        isAnnualExpense: Boolean,
        folderFilter: String
    ): Boolean {
        val filterKey = toKey(folderFilter)
        val filterCleanKey = toKey(cleanFolderName(folderFilter))
        val filterGroup = getCanonicalGroupId(folderFilter)

        val effKey = toKey(expenseEffectiveFolder)
        val effCleanKey = toKey(cleanFolderName(expenseEffectiveFolder))
        val custKey = expenseCustomFolder?.let { toKey(it) }
        val custCleanKey = expenseCustomFolder?.let { toKey(cleanFolderName(it)) }
        val titleKey = toKey(expenseTitle)
        val titleCleanKey = toKey(cleanFolderName(expenseTitle))

        val effGroup = getCanonicalGroupId(expenseEffectiveFolder)
        val custGroup = expenseCustomFolder?.let { getCanonicalGroupId(it) }
        val titleGroup = getCanonicalGroupId(expenseTitle)

        // 1. CAR MAINTENANCE: Strictly forbid annual commitments, insurance, motor items
        if (filterGroup == "CAR_MAINTENANCE") {
            if (isAnnualExpense) return false
            if (titleKey.contains("بیمه") || titleKey.contains("موتور")) return false
            if (effKey.contains("سالانه") || effKey.contains("بیمه") || effKey.contains("موتور")) return false
            return effGroup == "CAR_MAINTENANCE" || custGroup == "CAR_MAINTENANCE" || titleGroup == "CAR_MAINTENANCE"
        }

        // 2. MOTOR: Unifies all motorcycle expenses: monthly upkeep (repairs, fuel, service, oil)
        // PLUS annual motorcycle commitments (motor insurance, annual fees) amortized across the 12 months!
        if (filterGroup == "MOTOR" || filterGroup == "MOTOR_MAINTENANCE" || filterKey.contains("موتور")) {
            // Strictly exclude car items
            if (titleKey.contains("ماشین") || titleKey.contains("خودرو")) return false
            if (effKey.contains("ماشین") || effKey.contains("خودرو")) return false
            // Accept any motorcycle expense (both non-annual upkeep and amortized annual insurance)
            return effGroup == "MOTOR" || custGroup == "MOTOR" || titleGroup == "MOTOR" ||
                   effGroup == "MOTOR_MAINTENANCE" || custGroup == "MOTOR_MAINTENANCE" || titleGroup == "MOTOR_MAINTENANCE" ||
                   titleKey.contains("موتور") || effKey.contains("موتور") || (custKey != null && custKey.contains("موتور"))
        }

        // 3. ANNUAL COMMITMENTS
        if (filterGroup == "ANNUAL") {
            if (isAnnualExpense) return true
            return effGroup == "ANNUAL" || custGroup == "ANNUAL" || titleGroup == "ANNUAL"
        }

        // 4. TRANSPORT (Gasoline & Transport)
        if (filterGroup == "TRANSPORT") {
            if (isAnnualExpense) return false
            // Prevent common false matches like "تنقلات" containing "نقل"
            if (titleGroup == "SNACKS" || titleKey.contains("تنقلات")) return false
            if (titleGroup == "FRUIT" || titleGroup == "CHICKEN_MEAT" || titleGroup == "CLOTHING") return false
            return effGroup == "TRANSPORT" || custGroup == "TRANSPORT" || titleGroup == "TRANSPORT"
        }

        // 5. CLOTHING: Never allow fruits, chicken, supermarket, or snacks
        if (filterGroup == "CLOTHING") {
            if (isAnnualExpense) return false
            if (titleGroup == "FRUIT" || titleGroup == "CHICKEN_MEAT" || titleGroup == "SUPERMARKET" || titleGroup == "SNACKS") return false
            return effGroup == "CLOTHING" || custGroup == "CLOTHING" || titleGroup == "CLOTHING"
        }

        // 6. FRUIT: Strictly isolated to fruits and produce
        if (filterGroup == "FRUIT") {
            if (isAnnualExpense) return false
            if (titleGroup == "CHICKEN_MEAT" || titleGroup == "SNACKS" || titleGroup == "CLOTHING") return false
            return effGroup == "FRUIT" || custGroup == "FRUIT" || titleGroup == "FRUIT"
        }

        // 7. CHICKEN / MEAT: Strictly isolated to chicken and meat
        if (filterGroup == "CHICKEN_MEAT") {
            if (isAnnualExpense) return false
            if (titleGroup == "FRUIT" || titleGroup == "SNACKS" || titleGroup == "CLOTHING" || titleGroup == "SUPERMARKET") return false
            return effGroup == "CHICKEN_MEAT" || custGroup == "CHICKEN_MEAT" || titleGroup == "CHICKEN_MEAT"
        }

        // 8. SNACKS: Strictly isolated to snacks
        if (filterGroup == "SNACKS") {
            if (isAnnualExpense) return false
            if (titleGroup == "CHICKEN_MEAT" || titleGroup == "FRUIT" || titleGroup == "CLOTHING" || titleGroup == "TRANSPORT") return false
            return effGroup == "SNACKS" || custGroup == "SNACKS" || titleGroup == "SNACKS"
        }

        // 9. SUPERMARKET: For general grocery shopping, excludes items with their own dedicated category
        if (filterGroup == "SUPERMARKET") {
            if (isAnnualExpense) return false
            if (titleGroup == "CHICKEN_MEAT" || titleGroup == "FRUIT" || titleGroup == "SNACKS" || titleGroup == "CLOTHING") {
                // If it was explicitly tagged as SUPERMARKET custom folder by user, allow it
                if (custGroup == "SUPERMARKET") return true
                return false
            }
            return effGroup == "SUPERMARKET" || custGroup == "SUPERMARKET" || titleGroup == "SUPERMARKET"
        }

        // 10. RENT (Housing Rent - strictly separated from utilities/bills)
        if (filterGroup == "RENT") {
            if (isAnnualExpense) return false
            if (titleGroup == "BILLS") return false
            return effGroup == "RENT" || custGroup == "RENT" || titleGroup == "RENT"
        }

        // 11. BILLS (Utilities - strictly separated from rent)
        if (filterGroup == "BILLS") {
            if (isAnnualExpense) return false
            if (titleGroup == "RENT") return false
            return effGroup == "BILLS" || custGroup == "BILLS" || titleGroup == "BILLS"
        }

        // 12. General / Custom Folder (Exact key, cleaned key, or canonical group matching)
        if (effKey == filterKey || effCleanKey == filterCleanKey || effCleanKey == filterKey || effKey == filterCleanKey) return true
        if (custKey != null && (custKey == filterKey || custCleanKey == filterCleanKey || custCleanKey == filterKey || custKey == filterCleanKey)) return true
        if (titleKey == filterKey || titleCleanKey == filterCleanKey || titleCleanKey == filterKey || titleKey == filterCleanKey) return true

        if (effGroup == filterGroup) return true
        if (custGroup != null && custGroup == filterGroup) return true
        if (titleGroup == filterGroup) return true

        // Mutual containment for custom categories/names (e.g. filter "فاطمه" matching "پول فاطمه", "واریز به فاطمه")
        if (filterCleanKey.length >= 2) {
            if (effKey.contains(filterCleanKey) || titleKey.contains(filterCleanKey)) return true
            if (custKey != null && custKey.contains(filterCleanKey)) return true
        }

        return false
    }
}
