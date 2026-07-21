-- El trigger interno no requiere EXECUTE del invocador; se cierra el acceso vía RPC.
revoke execute on function public.handle_new_user() from anon, authenticated, public;
