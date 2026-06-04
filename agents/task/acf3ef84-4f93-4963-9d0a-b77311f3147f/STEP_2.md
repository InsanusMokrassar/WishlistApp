ENTITY:
entity_id=task_issue_24; type=feature_change; state=coding_complete

CONTEXT:
task_id=issue_24; agent_id=coding; protocol=AML-HIP
input_ref=STEP_1.md; branch=issue/24-items-amount

FILES_CHANGED:
1. features/wishlist/common/src/commonMain/kotlin/models/WishlistItem.kt — layer=model/DTO; added val amount:Int to sealed interface WishlistItem; override val amount:Int=1 in NewWishlistItem + RegisteredWishlistItem; KDoc @property added
2. features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt — layer=DB/serializer-jvm; amountColumn=integer("amount").default(1); read asObject(amount=get(amountColumn)); write update(it[amountColumn]=value.amount); insert InsertStatement.asObject(amount=value.amount); schema KDoc updated
3. features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt — layer=UI-strings; amountLabel ("Amount"/RU "Количество")
4. features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt — layer=UI-viewmodel; _amountState("1")+amountState; onAmountChanged(clamp>=1); load item.amount in init; onSave parse coerceAtLeast(1)?:1 → NewWishlistItem.amount
5. features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemEditView.kt — layer=UI-edit-js; Bootstrap Input(form-control,inputmode=numeric,min=1) amount field above price row
6. features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemEditView.kt — layer=UI-edit-jvm; OutlinedTextField amount above price row
7. features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemEditView.kt — layer=UI-edit-android; OutlinedTextField amount above price row
8. features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemCard.kt — layer=UI-card-js; Span badge bg-secondary "×{amount}" near title, gated item.amount!=1
9. features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemCard.kt — layer=UI-card-jvm; muted Text "×{amount}" under title, gated !=1
10. features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemCard.kt — layer=UI-card-android; muted Text "×{amount}" under title, gated !=1
11. features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemView.kt — layer=UI-detail-js; Amount section "×{amount}" gated !=1
12. features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemView.kt — layer=UI-detail-jvm; Amount section gated !=1
13. features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemView.kt — layer=UI-detail-android; Amount section gated !=1
14. features/wishlist/README.md — Models + Architecture Notes (amount Int=1, wishlist_items amount INT default 1, backward-compat)
15. features/ui/wishlist/README.md — Item Edit screen + amount Architecture Notes block

LAYERS_TOUCHED: model/DTO(1), DB(2), serializer(2+model-default), API(verify-only=no-change DTO passthrough server+client), UI-viewmodel(4), UI-edit(5,6,7), UI-card(8,9,10), UI-detail(11,12,13), strings(3), docs(14,15)

API_NO_CHANGE_JUSTIFICATION: server WishlistItemRoutingsConfigurator.receive<NewWishlistItem>() + client KtorWishlistItemFeature send/receive whole @Serializable DTO; amount flows automatically through kotlinx JSON

DEFAULT_1_AND_BACKWARD_COMPAT:
- serializer: @Serializable data class NewWishlistItem/RegisteredWishlistItem property amount:Int=1 → absent JSON field decodes to 1
- db: amountColumn=integer("amount").default(1) → SchemaUtils.createMissingTablesAndColumns adds column on existing wishlist_items with default 1
- viewmodel: onAmountChanged + onSave clamp coerceAtLeast(1); empty/invalid → 1
- invariant: amount>=1 everywhere

CARD_HIDE_AMOUNT_EQ_1:
- js card: if(item.amount!=1){Span badge "×{amount}"}
- jvm card: if(item.amount!=1){Text "×{amount}"}
- android card: if(item.amount!=1){Text "×{amount}"}
- detail views same gate

BUILD_VERIFY:
- cmd1=./gradlew :wishlist.features.ui.wishlist:build (broadest affected; JS+JVM+Android) → BUILD SUCCESSFUL (52s)
- cmd2=./gradlew :wishlist.features.wishlist.server:build (DB/serializer JVM persistence) → BUILD SUCCESSFUL (19s)
- ast-index rebuild → indexed 507 files, 35 modules
- fix_cycles=0 (no compile errors)

NOTE_JS_INPUT: InputType.Number generic=Number? incompatible with String viewmodel state; used InputType.Text + inputmode=numeric + min=1 (matches existing price/weight text-input idiom in same view)

EXPECTED_RESULT:
entity_id=task_issue_24; new_state=complete; location=STEP_2.md

VERIFICATION:
check=build_ui_wishlist; expected=SUCCESS; actual=SUCCESS
check=build_wishlist_server; expected=SUCCESS; actual=SUCCESS
check=all_layers_present; expected=true; actual=true

REPETITION_OF_RESULT:
entity_id=task_issue_24; stored_in=STEP_2.md; status=available; build=success
