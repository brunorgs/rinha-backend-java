CREATE TABLE public.payment (
	amount numeric(4, 2) NULL,
	requested_at TIMESTAMP WITH TIME ZONE NULL,
	id SERIAL PRIMARY KEY
);

CREATE INDEX idx_payment_requested_at ON payment(requested_at);

CREATE TABLE public.payment_fallback (
	amount numeric(4, 2) NULL,
	requested_at TIMESTAMP WITH TIME ZONE NULL,
	id SERIAL PRIMARY KEY
);

CREATE INDEX idx_payment_fallback_requested_at ON payment_fallback(requested_at);