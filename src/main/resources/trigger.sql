
-- populate checkouts_history
CREATE TRIGGER trigger_audit_checkouts
    AFTER INSERT OR UPDATE OR DELETE ON checkouts
    FOR EACH ROW
EXECUTE FUNCTION audit_checkouts();


-- update "updated_at" column
CREATE TRIGGER update_schools_updated_at
    BEFORE UPDATE ON schools
    FOR EACH ROW
EXECUTE FUNCTION moddatetime(updated_at);

CREATE TRIGGER update_book_categories_updated_at
    BEFORE UPDATE ON book_categories
    FOR EACH ROW
EXECUTE FUNCTION moddatetime(updated_at);

CREATE TRIGGER update_books_updated_at
    BEFORE UPDATE ON books
    FOR EACH ROW
EXECUTE FUNCTION moddatetime(updated_at);

CREATE TRIGGER update_members_updated_at
    BEFORE UPDATE ON members
    FOR EACH ROW
EXECUTE FUNCTION moddatetime(updated_at);

CREATE TRIGGER update_checkouts_updated_at
    BEFORE UPDATE ON checkouts
    FOR EACH ROW
EXECUTE FUNCTION moddatetime(updated_at);

CREATE TRIGGER update_limits_updated_at
    BEFORE UPDATE ON limits
    FOR EACH ROW
EXECUTE FUNCTION moddatetime(updated_at);

CREATE TRIGGER update_activities_updated_at
    BEFORE UPDATE ON activities
    FOR EACH ROW
EXECUTE FUNCTION moddatetime(updated_at);

CREATE TRIGGER update_checkout_limit_defaults_updated_at
    BEFORE UPDATE ON checkout_limit_defaults
    FOR EACH ROW
EXECUTE FUNCTION moddatetime(updated_at);

CREATE TRIGGER update_checkout_limit_schedules_updated_at
    BEFORE UPDATE ON checkout_limit_schedules
    FOR EACH ROW
EXECUTE FUNCTION moddatetime(updated_at);
