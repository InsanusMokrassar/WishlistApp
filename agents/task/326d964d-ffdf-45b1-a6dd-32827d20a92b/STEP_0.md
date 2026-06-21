ENTITY:
entity_id=issue-54; type=github_issue; state=fixed
title="Make breadcrumbs clickable"

CONTEXT:
* task_id=326d964d-ffdf-45b1-a6dd-32827d20a92b; role=issue-executor
* repo=InsanusMokrassar/WishlistApp; branch=issue/54-clickable-breadcrumbs
* constraints=[CODING.md, Calm Studio design rules, no else-if, KDoc, README update]

REQUIREMENT:
* condition=click on breadcrumb segment -> navigate to chain point that segment reflects

ACTION:
1. action=add_method; target=TopBarViewModel; params={onCrumbSelected(provider: TopBarTitleProvider), field mainChain:NavigationChain<ViewConfig>?}
   detail=collapse main chain by dropping nodes above provider node (top-down, await each removal via stackFlow.first)
2. action=edit_view; target=features/ui/topBar/src/jsMain/.../TopBarView.kt
   detail=render non-last crumbs as <a> with onClick->onCrumbSelected(provider); last stays <b>; uses existing `.crumb a` CSS
3. action=update_docs; target=features/ui/topBar/README.md params={breadcrumb clickable note, ViewModel events row}

SCOPE_NOTE:
* breadcrumb (.crumb) is rendered only by JS (web/Calm Studio) TopBarView -> clickability implemented for JS. JVM/Android top bar do not render segmented crumbs.

VERIFICATION:
* check=`./gradlew :wishlist.features.ui.topBar:build`; expected=BUILD SUCCESSFUL; result=BUILD SUCCESSFUL (41s)
* check=ast-index rebuild; result=indexed 618 files

EXPECTED RESULT:
* entity_id=issue-54; new_state=ready_for_PR; location=branch issue/54-clickable-breadcrumbs
