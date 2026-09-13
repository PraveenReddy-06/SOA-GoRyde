function RegisterPage() {
  return <PlaceholderPage title="Register" description="Your account registration experience will be added here." />
}

function PlaceholderPage({ title, description }) {
  return (
    <section className="mx-auto max-w-2xl px-6 py-20 text-center">
      <h1 className="text-4xl font-bold tracking-tight text-slate-950">{title}</h1>
      <p className="mt-4 text-lg text-slate-600">{description}</p>
    </section>
  )
}

export default RegisterPage