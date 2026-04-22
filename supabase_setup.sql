-- ============================================================
-- VetCarnet — Script de configuration Supabase
-- À exécuter dans l'éditeur SQL de votre projet Supabase
-- ============================================================

-- 1. TABLE : dossiers_animaux
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.dossiers_animaux (
    id                      TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    nom                     TEXT NOT NULL,
    espece                  TEXT NOT NULL CHECK (espece IN (
                                'CHIEN','CHAT','PORC','LAPIN',
                                'OISEAU','REPTILE','BOVINS','AUTRE'
                            )),
    race                    TEXT DEFAULT '',
    nom_proprietaire        TEXT NOT NULL,
    telephone_proprietaire  TEXT DEFAULT '',
    date_naissance          DATE,
    date_prochain_vaccin    DATE,
    photo_url               TEXT,
    notes                   TEXT DEFAULT '',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Trigger pour mettre à jour updated_at automatiquement
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_dossiers_animaux_updated_at
    BEFORE UPDATE ON public.dossiers_animaux
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- 2. ROW LEVEL SECURITY (RLS)
-- ─────────────────────────────────────────────────────────────
-- Pour un usage simple (un seul vétérinaire, clé anon), 
-- on autorise tout depuis la clé anon.
-- Pour une vraie app multi-utilisateurs, ajouter l'auth.

ALTER TABLE public.dossiers_animaux ENABLE ROW LEVEL SECURITY;

-- Politique permissive (anon key)
CREATE POLICY "Allow all for anon" ON public.dossiers_animaux
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- 3. INDEX pour optimiser les requêtes fréquentes
-- ─────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_dossiers_vaccin
    ON public.dossiers_animaux (date_prochain_vaccin)
    WHERE date_prochain_vaccin IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_dossiers_proprietaire
    ON public.dossiers_animaux (nom_proprietaire);

-- 4. STORAGE BUCKET : animal-photos
-- ─────────────────────────────────────────────────────────────
-- À exécuter via le Dashboard Supabase > Storage > New bucket
-- Ou via ce SQL :

INSERT INTO storage.buckets (id, name, public)
VALUES ('animal-photos', 'animal-photos', true)
ON CONFLICT (id) DO NOTHING;

-- Politique de stockage : autoriser upload/lecture avec clé anon
CREATE POLICY "Public read animal photos"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'animal-photos');

CREATE POLICY "Anon upload animal photos"
    ON storage.objects FOR INSERT
    WITH CHECK (bucket_id = 'animal-photos');

CREATE POLICY "Anon update animal photos"
    ON storage.objects FOR UPDATE
    USING (bucket_id = 'animal-photos');

CREATE POLICY "Anon delete animal photos"
    ON storage.objects FOR DELETE
    USING (bucket_id = 'animal-photos');

-- 5. DONNÉES DE TEST (optionnel)
-- ─────────────────────────────────────────────────────────────
INSERT INTO public.dossiers_animaux
    (nom, espece, race, nom_proprietaire, telephone_proprietaire, date_naissance, date_prochain_vaccin, notes)
VALUES
    ('Rex',    'CHIEN',  'Berger Allemand', 'Jean Dupont',     '0612345678', '2021-03-15', CURRENT_DATE + INTERVAL '1 day', 'Rappel vaccin rage + CHPPI'),
    ('Mimi',   'CHAT',   'Siamois',         'Marie Martin',    '0698765432', '2020-07-22', CURRENT_DATE,                     'Stérilisée. Traitement antipuces mensuel.'),
    ('Goliath','PORC',   'Landrace',        'Pierre Ferme',    '0611223344', '2022-01-10', CURRENT_DATE + INTERVAL '7 days', 'Contrôle poids à chaque visite.'),
    ('Luna',   'LAPIN',  'Bélier',          'Sophie Leblanc',  '0655443322', '2023-05-01', CURRENT_DATE + INTERVAL '30 days','Première vaccination.');
