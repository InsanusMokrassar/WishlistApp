# Source Prompt

1. Lets remove `Подтверждение email` subtitle from profile editing page
2. Lets add opportunity for user to change password - request email for deeplink which will lead to password change page. After opening deeplink, handler must rediract onto page of changing password. Password change page must use the same approvement uuid as deeplink it came from for confirmation of user password change access allowance. This option must be available only in case when server email service have been setup and user have approved email
3. Lets add opportunity for user to change his email

For each point create an issue on GH and init handling of github unresolved issues without PR
