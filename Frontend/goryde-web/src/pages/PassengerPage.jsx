function PassengerPage() {
  return <DashboardPlaceholder title="Passenger dashboard" description="Passenger tools will be added here." />
}

function DashboardPlaceholder({ title, description }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-10 shadow-sm">
      <h1 className="text-3xl font-bold tracking-tight text-slate-950">{title}</h1>
      <p className="mt-3 text-slate-600">{description}</p>
    </section>
  )
}

export default PassengerPage