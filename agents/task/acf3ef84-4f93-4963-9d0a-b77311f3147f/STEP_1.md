ENTITY:
entity_id=task_issue_24; type=feature_change; state=architecture_defined

CONTEXT:
task_id=issue_24; agent_id=architecture; protocol=AML-HIP
input_ref=STEP_0.md

DECISION_TYPE: amount:Int (matches issue "integer"); default=1; constraint amount>=1

ARCH_LAYER_MODEL:
file=features/wishlist/common/src/commonMain/kotlin/models/WishlistItem.kt
action=add property val amount:Int to sealed interface WishlistItem; add override val amount:Int=1 to NewWishlistItem + RegisteredWishlistItem (default ensures @Serializable backward-compat: absent JSON field→1)
kdoc=required(every property)

ARCH_LAYER_DB:
file=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt
action=add private val amountColumn=integer("amount").default(1)
read: asObject→amount=get(amountColumn)
write: update(it)→it[amountColumn]=value.amount
insert: InsertStatement.asObject→amount=value.amount
backward_compat=.default(1) → existing rows + SchemaUtils.createMissingTablesAndColumns adds column w/ default 1
schema_doc_comment=update class kdoc schema list

ARCH_LAYER_SERIALIZER: automatic (kotlinx @Serializable data class default=1); no custom serializer (unlike Priority which needs weight mapping)

ARCH_LAYER_API: NONE; server WishlistItemRoutingsConfigurator + client KtorWishlistItemFeature pass whole DTO; verify_only

ARCH_LAYER_UI_VIEWMODEL:
file=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt
action=add _amountState:MutableRedeliverStateFlow<String>("1") + val amountState; onAmountChanged(v:String) clamps min1 (parse toIntOrNull; coerceAtLeast(1); store as string; set dirty); load in init (item.amount.toString()); onSave parse _amountState.value.toIntOrNull()?.coerceAtLeast(1)?:1 → NewWishlistItem(amount=...)
default_create_mode=amount "1"

ARCH_LAYER_UI_EDIT:
files=features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemEditView.kt
js=Input(InputType.Number) classes form-control; attr min=1; value=amount; onInput onAmountChanged; wrapped Div mb-3 + Label amountLabel; placed after price row
jvm=OutlinedTextField value=amount label=amountLabel singleLine enabled=!loading
android=OutlinedTextField same w/ translation(resources)

ARCH_LAYER_UI_CARD:
files=features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemCard.kt
rule=render amount ONLY if item.amount!=1
format="×{amount}" (count badge)
js=add Span/Div badge in card-body near title (classes badge bg-secondary ms-2) when item.amount!=1
jvm=Text("×${item.amount}") caption when !=1, inline after title row
android=Text("×${item.amount}") bodySmall when !=1

ARCH_LAYER_UI_DETAIL:
files=features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemView.kt
rule=show amount section only when item.amount!=1 (consistency w/ issue card rule; detail optional but mirror)
js=Div mb-3 H6 amountLabel + P "×{amount}" when !=1
jvm/android=same pattern as other sections, gated !=1

ARCH_LAYER_STRINGS:
file=features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt
action=add amountLabel=buildStringResource("Amount"){IetfLang.Russian("Количество")}

ARCH_README_UPDATES:
features/wishlist/README.md: Models NewWishlistItem/RegisteredWishlistItem add amount:Int=1; Architecture Notes wishlist_items table add amount INT default 1
features/ui/wishlist/README.md: Item Edit screen + card note amount field (min 1, card shows only when !=1)

INVARIANTS:
- amount>=1 always (clamped viewmodel + db default)
- card/detail hide amount when ==1

EXPECTED_RESULT:
entity_id=task_issue_24; new_state=coding_pending; location=STEP_2.md

VERIFICATION:
check=all_layers_addressed; expected=true
check=broadest_module=:wishlist.features.ui.wishlist build target

REPETITION_OF_RESULT:
entity_id=task_issue_24; stored_in=STEP_1.md; status=available
